package com.example.tracker.ui.workout

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
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
import com.example.tracker.domain.model.SetType
import com.example.tracker.domain.repository.SettingsRepository
import com.example.tracker.domain.repository.WorkoutRepository
import com.example.tracker.domain.workout.RestAlarm
import com.example.tracker.domain.workout.restAfterCompleting
import com.example.tracker.domain.workout.resumeExerciseIndex
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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * Active workout: one exercise at a time, logging its sets.
 *
 * Saved sets come from the repository; what the user is choosing, typing or timing lives
 * here as [WorkoutSelection] and [SetDraft] until it is saved, backed by [savedState] so it
 * survives Android ending the app in the background. The open exercise is also stored with
 * the workout, so the session reopens on it even after a restart. Completing a set starts a
 * rest countdown as long as Settings' rest times say; [restAlarm] tells the user when it is
 * over while the app is in the background. State is exposed as [uiState];
 * navigation and messages are sent once through [events].
 */
// One function per action on the workout screen; the rest timer is the natural piece to split out next.
@Suppress("TooManyFunctions")
@OptIn(ExperimentalCoroutinesApi::class)
class ActiveWorkoutViewModel(
    private val workoutId: String,
    private val workoutRepository: WorkoutRepository,
    private val settingsRepository: SettingsRepository,
    private val restAlarm: RestAlarm,
    private val time: TimeProvider,
    private val savedState: SavedStateHandle = SavedStateHandle(),
) : ViewModel() {

    private val loadAttempt = MutableStateFlow(0)
    private val selection = MutableStateFlow(savedState.readSelection())
    private val draft = MutableStateFlow(savedState.readDraft())
    private val isFinishing = MutableStateFlow(false)
    private val rest = MutableStateFlow(savedState.readRest())

    private val _events = Channel<ActiveWorkoutEvent>(Channel.BUFFERED)
    val events: Flow<ActiveWorkoutEvent> = _events.receiveAsFlow()

    val uiState: StateFlow<ActiveWorkoutUiState> = loadAttempt
        .flatMapLatest {
            combine(
                workoutRepository.observeWorkout(workoutId),
                observeLastTimes(),
                selection,
                draft,
                combine(isFinishing, rest, ::Pair),
            ) { workout, lastTimes, selection, draft, (finishing, rest) ->
                val state = ActiveWorkoutStateMapper.map(workout, lastTimes, selection, draft)
                if (state is ActiveWorkoutUiState.Success) state.copy(isFinishing = finishing, rest = rest) else state
            }
                .onStart { emit(ActiveWorkoutUiState.Loading) }
                .catch { emit(ActiveWorkoutUiState.Error) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ActiveWorkoutUiState.Loading)

    init {
        selection.onEach(savedState::writeSelection).launchIn(viewModelScope)
        draft.onEach(savedState::writeDraft).launchIn(viewModelScope)
        rest.onEach(savedState::writeRest).launchIn(viewModelScope)
        // Pin the starting exercise, so completing its last set doesn't jump to the next one.
        viewModelScope.launchCatching(onError = {}) {
            val start = workoutRepository.observeWorkout(workoutId).filterNotNull().first().resumeExerciseIndex()
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
            startRestAfter(active.id)
            _events.send(ActiveWorkoutEvent.SetCompleted(active.id, active.number, active.type))
        }
    }

    /** Undoes [onCompleteSet]: the set becomes active again, and its rest is dropped. */
    fun onUndoCompleteSet(setId: String) {
        val row = currentSets().firstOrNull { it.id == setId } as? SetRowUi.Done
        rest.value = null
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

    fun onAddSet() = addSet(SetType.NORMAL)

    /** Adds a warm-up before the working sets. */
    fun onAddWarmUpSet() = addSet(SetType.WARM_UP)

    /** Marks the active set as a warm-up, drop set, set to failure or a normal set. */
    fun onSetActiveSetType(type: SetType) {
        val active = activeSet() ?: return
        if (active.type == type) return
        viewModelScope.launchCatching(onError = { showMessage(R.string.workout_error_save) }) {
            workoutRepository.setSetType(active.id, type)
        }
    }

    /** Rates how hard the active set felt; null clears the rating. */
    fun onSetActiveRpe(rpe: Double?) {
        val active = activeSet() ?: return
        if (active.rpe == rpe) return
        viewModelScope.launchCatching(onError = { showMessage(R.string.workout_error_save) }) {
            workoutRepository.setSetRpe(active.id, rpe)
        }
    }

    private fun addSet(type: SetType) {
        val state = uiState.value as? ActiveWorkoutUiState.Success ?: return
        viewModelScope.launchCatching(onError = { showMessage(R.string.workout_error_save) }) {
            workoutRepository.addSet(state.exercise.workoutExerciseId, type)
        }
    }

    /** Lengthens (or with a negative value, shortens) the rest; shortening it past zero ends it. */
    fun onAdjustRest(seconds: Int) {
        val now = time.now()
        rest.update { current ->
            val endsAt = current?.endsAt?.plusSeconds(seconds.toLong())
            if (endsAt == null || !endsAt.isAfter(now)) {
                null
            } else {
                current.copy(endsAt = endsAt, totalSeconds = current.totalSeconds + seconds)
            }
        }
    }

    /** The rest was skipped or has run out. */
    fun onEndRest() {
        rest.value = null
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

    /**
     * The app went to the background, or the workout was minimized. A running rest sets an
     * alarm for its end. Typed weight and reps are stored with their set, so they are kept
     * even if the saved state is lost too, e.g. when the app is swiped away. The draft stays,
     * so nothing changes on screen.
     */
    fun onAppBackgrounded() {
        rest.value
            ?.takeIf { it.endsAt.isAfter(time.now()) }
            ?.let { restAlarm.schedule(workoutId, it.kind, it.endsAt) }
        val pending = draft.value as? SetDraft.WeightReps ?: return
        val values = completedValues(SetEditorUi.WeightReps(pending.weightText, pending.repsText)) ?: return
        viewModelScope.launchCatching(onError = {}) {
            workoutRepository.updateSet(pending.setId, values.weightKg, values.reps, values.durationSeconds)
        }
    }

    /** Back on screen: the countdown shows again, so its alarm and notification go. */
    fun onAppForegrounded() {
        restAlarm.cancel()
    }

    private fun goToExercise(offset: Int) {
        val state = uiState.value as? ActiveWorkoutUiState.Success ?: return
        val target = state.exerciseIndex + offset
        if (target !in 0 until state.exerciseCount) return
        viewModelScope.launchCatching(onError = { showMessage(R.string.workout_error_save) }) {
            saveDraft()
            selection.value = WorkoutSelection(exerciseIndex = target)
            workoutRepository.setCurrentExercise(workoutId, target)
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

    /** Starts the rest that follows completing [setId], or clears the last one if none follows. */
    private suspend fun startRestAfter(setId: String) {
        val workout = workoutRepository.observeWorkout(workoutId).first()
        val period = workout?.restAfterCompleting(setId, settingsRepository.restTimes.value)
        rest.value = period?.let { RestUi(it.kind, time.now().plusSeconds(it.seconds.toLong()), it.seconds) }
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
                val savedState = createSavedStateHandle()
                ActiveWorkoutViewModel(
                    workoutId = savedState.toRoute<ActiveWorkoutDestination>().workoutId,
                    workoutRepository = container.workoutRepository,
                    settingsRepository = container.settingsRepository,
                    restAlarm = container.restAlarm,
                    time = container.time,
                    savedState = savedState,
                )
            }
        }
    }
}
