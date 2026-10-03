package com.example.tracker.ui.workout

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import com.example.tracker.R
import com.example.tracker.TrackerApplication
import com.example.tracker.core.TimeProvider
import com.example.tracker.domain.repository.WorkoutRepository
import com.example.tracker.ui.common.SetValueUi
import com.example.tracker.ui.common.launchCatching
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * Active workout: one exercise at a time, logging its sets.
 *
 * Saved sets come from the repository; what the user is choosing, typing or timing lives
 * here as [WorkoutSelection] and [SetDraft] until it is saved. State is exposed as [uiState];
 * navigation and messages are sent once through [events].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ActiveWorkoutViewModel(
    private val workoutId: String,
    private val workoutRepository: WorkoutRepository,
    private val time: TimeProvider,
) : ViewModel() {

    private val loadAttempt = MutableStateFlow(0)
    private val selection = MutableStateFlow(WorkoutSelection())
    private val draft = MutableStateFlow<SetDraft?>(null)
    private val isFinishing = MutableStateFlow(false)

    private val _events = Channel<ActiveWorkoutEvent>(Channel.BUFFERED)
    val events: Flow<ActiveWorkoutEvent> = _events.receiveAsFlow()

    val uiState: StateFlow<ActiveWorkoutUiState> = loadAttempt
        .flatMapLatest {
            combine(
                workoutRepository.observeWorkout(workoutId),
                observeLastTimes(),
                selection,
                draft,
                isFinishing,
            ) { workout, lastTimes, selection, draft, finishing ->
                val state = ActiveWorkoutStateMapper.map(workout, lastTimes, selection, draft)
                if (state is ActiveWorkoutUiState.Success) state.copy(isFinishing = finishing) else state
            }
                .onStart { emit(ActiveWorkoutUiState.Loading) }
                .catch { emit(ActiveWorkoutUiState.Error) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ActiveWorkoutUiState.Loading)

    init {
        // Pin the starting exercise, so completing its last set doesn't jump to the next one.
        viewModelScope.launchCatching(onError = {}) {
            val workout = workoutRepository.observeWorkout(workoutId).filterNotNull().first()
            val start = ActiveWorkoutStateMapper.defaultExerciseIndex(workout)
            selection.update { if (it.exerciseIndex == null) it.copy(exerciseIndex = start) else it }
        }
    }

    fun onRetry() {
        loadAttempt.update { it + 1 }
    }

    fun onWeightTextChange(text: String) = editActive { it.copy(weightText = text) }

    fun onRepsTextChange(text: String) = editActive { it.copy(repsText = text) }

    fun onWeightStep(steps: Int) = editActive { it.copy(weightText = SetInput.stepWeight(it.weightText, steps)) }

    fun onRepsStep(steps: Int) = editActive { it.copy(repsText = SetInput.stepReps(it.repsText, steps)) }

    /** Starts, pauses or resumes the active hold's timer. */
    fun onToggleTimer() = editHold { hold, now ->
        if (hold.runningSince == null) {
            hold.copy(runningSince = now)
        } else {
            hold.copy(bankedSeconds = hold.secondsAt(now), runningSince = null)
        }
    }

    /** Adds (or with a negative value, removes) seconds from the active hold, never below 0. */
    fun onAdjustHold(seconds: Int) = editHold { hold, now ->
        val adjusted = (hold.secondsAt(now) + seconds).coerceAtLeast(0)
        hold.copy(bankedSeconds = adjusted, runningSince = hold.runningSince?.let { now })
    }

    fun onCompleteSet() {
        val active = activeSet() ?: return
        val values = completedValues(active.editor) ?: return
        viewModelScope.launchCatching(onError = { showMessage(R.string.workout_error_save) }) {
            workoutRepository.updateSet(active.id, values.weightKg, values.reps, values.durationSeconds)
            workoutRepository.completeSet(active.id)
            draft.value = null
            selection.update { it.copy(setId = null) }
            _events.send(ActiveWorkoutEvent.SetCompleted(active.id, active.number))
        }
    }

    /** Undoes [onCompleteSet]: the set becomes active again. */
    fun onUndoCompleteSet(setId: String) {
        val row = currentSets().firstOrNull { it.id == setId } as? SetRowUi.Done
        viewModelScope.launchCatching(onError = { showMessage(R.string.workout_error_save) }) {
            saveDraft()
            reopen(setId, row)
        }
    }

    /** Makes [setId] the active set; a completed set is reopened for editing. */
    fun onSelectSet(setId: String) {
        val row = currentSets().firstOrNull { it.id == setId } ?: return
        if (row is SetRowUi.Active) return
        viewModelScope.launchCatching(onError = { showMessage(R.string.workout_error_save) }) {
            saveDraft()
            if (row is SetRowUi.Done) reopen(setId, row) else selection.update { it.copy(setId = setId) }
        }
    }

    fun onRemoveActiveSet() {
        val active = activeSet() ?: return
        viewModelScope.launchCatching(onError = { showMessage(R.string.workout_error_save) }) {
            workoutRepository.deleteSet(active.id)
            draft.value = null
            selection.update { it.copy(setId = null) }
        }
    }

    fun onAddSet() {
        val state = uiState.value as? ActiveWorkoutUiState.Success ?: return
        viewModelScope.launchCatching(onError = { showMessage(R.string.workout_error_save) }) {
            workoutRepository.addSet(state.exercise.workoutExerciseId)
        }
    }

    fun onPreviousExercise() = goToExercise(offset = -1)

    fun onNextExercise() = goToExercise(offset = 1)

    fun onFinishWorkout() = endWorkout(R.string.workout_error_finish) {
        saveDraft()
        workoutRepository.finishWorkout(workoutId)
    }

    fun onDiscardWorkout() = endWorkout(R.string.workout_error_finish) {
        workoutRepository.discardWorkout(workoutId)
    }

    private fun goToExercise(offset: Int) {
        val state = uiState.value as? ActiveWorkoutUiState.Success ?: return
        val target = state.exerciseIndex + offset
        if (target !in 0 until state.exerciseCount) return
        viewModelScope.launchCatching(onError = { showMessage(R.string.workout_error_save) }) {
            saveDraft()
            selection.value = WorkoutSelection(exerciseIndex = target)
        }
    }

    private fun endWorkout(@StringRes errorRes: Int, block: suspend () -> Unit) {
        if (isFinishing.value) return
        isFinishing.value = true
        viewModelScope.launchCatching(onError = { showMessage(errorRes) }) {
            try {
                block()
                _events.send(ActiveWorkoutEvent.WorkoutEnded)
            } finally {
                isFinishing.value = false
            }
        }
    }

    /** Makes a completed set active again; a hold continues from its recorded time. */
    private suspend fun reopen(setId: String, row: SetRowUi.Done?) {
        val hold = row?.value as? SetValueUi.Hold
        if (hold != null) draft.value = SetDraft.Hold(setId, hold.seconds, runningSince = null)
        selection.update { it.copy(setId = setId) }
        workoutRepository.reopenSet(setId)
    }

    /** Applies [change] to the active set's typed weight and reps. */
    private fun editActive(change: (SetDraft.WeightReps) -> SetDraft.WeightReps) {
        val active = activeSet() ?: return
        val editor = active.editor as? SetEditorUi.WeightReps ?: return
        draft.value = change(SetDraft.WeightReps(active.id, editor.weightText, editor.repsText))
    }

    /** Applies [change] to the active hold, given the current time. */
    private fun editHold(change: (SetEditorUi.Duration, Instant) -> SetEditorUi.Duration) {
        val active = activeSet() ?: return
        val editor = active.editor as? SetEditorUi.Duration ?: return
        val changed = change(editor, time.now())
        draft.value = SetDraft.Hold(active.id, changed.bankedSeconds, changed.runningSince)
    }

    /** Saves the active set's unsaved input if it is valid, then forgets the draft. */
    private suspend fun saveDraft() {
        val pending = draft.value ?: return
        draft.value = null
        val values = when (pending) {
            is SetDraft.WeightReps -> completedValues(SetEditorUi.WeightReps(pending.weightText, pending.repsText))
            is SetDraft.Hold -> completedValues(SetEditorUi.Duration(pending.bankedSeconds, pending.runningSince, null))
        } ?: return
        workoutRepository.updateSet(pending.setId, values.weightKg, values.reps, values.durationSeconds)
    }

    /** What the editor's input saves as, or null if it isn't a loggable set yet. */
    private fun completedValues(editor: SetEditorUi): SetValues? = when (editor) {
        is SetEditorUi.WeightReps -> {
            val weight = editor.weightKg
            val reps = editor.reps
            if (weight != null && reps != null) SetValues(weight, reps, null) else null
        }

        is SetEditorUi.Duration -> editor.secondsAt(time.now()).takeIf { it > 0 }?.let { SetValues(null, null, it) }
    }

    private data class SetValues(val weightKg: Double?, val reps: Int?, val durationSeconds: Int?)

    private fun currentSets(): List<SetRowUi> =
        (uiState.value as? ActiveWorkoutUiState.Success)?.sets.orEmpty()

    private fun activeSet(): SetRowUi.Active? = currentSets().filterIsInstance<SetRowUi.Active>().firstOrNull()

    private fun observeLastTimes() = workoutRepository.observeWorkout(workoutId)
        .map { workout -> workout?.exercises?.map { it.exercise.id }?.toSet().orEmpty() }
        .distinctUntilChanged()
        .map { exerciseIds -> exerciseIds.associateWith { workoutRepository.lastTimeSets(it) } }

    private suspend fun showMessage(@StringRes messageRes: Int) {
        _events.send(ActiveWorkoutEvent.ShowMessage(messageRes))
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as TrackerApplication).container
                ActiveWorkoutViewModel(
                    workoutId = createSavedStateHandle().toRoute<ActiveWorkoutDestination>().workoutId,
                    workoutRepository = container.workoutRepository,
                    time = container.time,
                )
            }
        }
    }
}
