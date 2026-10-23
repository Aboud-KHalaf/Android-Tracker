package com.example.tracker.ui.plan

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
import com.example.tracker.domain.model.ExerciseConfig
import com.example.tracker.domain.model.ExerciseType
import com.example.tracker.domain.model.MuscleGroup
import com.example.tracker.domain.repository.ExerciseRepository
import com.example.tracker.domain.repository.PlanRepository
import com.example.tracker.domain.repository.WorkoutRepository
import com.example.tracker.ui.common.launchCatching
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * Training day screen: the day's exercises in order, editing, replacing and reordering them,
 * their targets, and starting a workout. State is exposed as [uiState]; navigation and
 * messages are sent once through [events].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PlanViewModel(
    private val planId: String,
    private val planRepository: PlanRepository,
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
    time: TimeProvider,
) : ViewModel() {

    private val mapper = PlanStateMapper(time.zone())
    private val loadAttempt = MutableStateFlow(0)
    private val isStartingWorkout = MutableStateFlow(false)
    private val editing = MutableStateFlow<EditTarget?>(null)

    private val _events = Channel<PlanEvent>(Channel.BUFFERED)
    val events: Flow<PlanEvent> = _events.receiveAsFlow()

    val uiState: StateFlow<PlanUiState> = loadAttempt
        .flatMapLatest {
            combine(observeContent(), isStartingWorkout) { state, starting ->
                if (state is PlanUiState.Success) state.copy(isStartingWorkout = starting) else state
            }
                .onStart { emit(PlanUiState.Loading) }
                .catch { emit(PlanUiState.Error) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), PlanUiState.Loading)

    fun onRetry() {
        loadAttempt.update { it + 1 }
    }

    fun onStartWorkout() {
        if (isStartingWorkout.value) return
        isStartingWorkout.value = true
        viewModelScope.launchCatching(onError = { showMessage(R.string.plan_error_start_workout) }) {
            try {
                val workoutId = workoutRepository.startWorkout(planId)
                _events.send(PlanEvent.OpenWorkout(workoutId))
            } finally {
                isStartingWorkout.value = false
            }
        }
    }

    fun onMoveUp(index: Int) = onMoveExercise(index, index - 1)

    fun onMoveDown(index: Int) = onMoveExercise(index, index + 1)

    /** Saves a new position for an exercise, e.g. after dragging it. */
    fun onMoveExercise(fromIndex: Int, toIndex: Int) {
        val count = (uiState.value as? PlanUiState.Success)?.exercises?.size ?: return
        if (fromIndex == toIndex || fromIndex !in 0 until count || toIndex !in 0 until count) return
        viewModelScope.launchCatching(onError = { showMessage(R.string.plan_error_update) }) {
            planRepository.moveExercise(planId, fromIndex, toIndex)
        }
    }

    /**
     * Swaps the library exercise behind [planExerciseId], keeping its place and targets.
     * [muscleGroup] is given when the exercise had none yet.
     */
    fun onReplaceExercise(planExerciseId: String, exerciseId: String, muscleGroup: MuscleGroup? = null) {
        viewModelScope.launchCatching(onError = { showMessage(R.string.plan_error_replace) }) {
            if (muscleGroup != null) exerciseRepository.setMuscleGroup(exerciseId, muscleGroup)
            planRepository.replaceExercise(planExerciseId, exerciseId)
        }
    }

    /** Creates a new library exercise and puts it in place of [planExerciseId]. */
    fun onCreateReplacement(planExerciseId: String, name: String, type: ExerciseType, muscleGroup: MuscleGroup) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launchCatching(onError = { showMessage(R.string.plan_error_replace) }) {
            val exerciseId = exerciseRepository.createExercise(trimmed, type, muscleGroup)
            planRepository.replaceExercise(planExerciseId, exerciseId)
        }
    }

    /** Opens the targets of one exercise, alongside what was recorded for it last time. */
    fun onEditExercise(planExerciseId: String) {
        val exerciseId = (uiState.value as? PlanUiState.Success)
            ?.exercises?.firstOrNull { it.id == planExerciseId }?.exerciseId
            ?: return
        viewModelScope.launchCatching(onError = { showMessage(R.string.plan_error_load_targets) }) {
            editing.value = EditTarget(planExerciseId, workoutRepository.lastTimeSets(exerciseId))
        }
    }

    fun onDismissEditor() {
        editing.value = null
    }

    /** Saves one exercise's targets; the day's other exercises are untouched. */
    fun onSaveExerciseConfig(planExerciseId: String, config: ExerciseConfig) {
        val target = editing.value?.takeIf { it.planExerciseId == planExerciseId && !it.isSaving } ?: return
        editing.value = target.copy(isSaving = true)
        viewModelScope.launchCatching(
            onError = {
                editing.update { it?.copy(isSaving = false) }
                showMessage(R.string.plan_error_save_config)
            },
        ) {
            planRepository.updateExerciseConfig(planExerciseId, config)
            editing.value = null
        }
    }

    fun onRemoveExercise(planExerciseId: String) {
        val name = (uiState.value as? PlanUiState.Success)
            ?.exercises?.firstOrNull { it.id == planExerciseId }?.name
            ?: return
        viewModelScope.launchCatching(onError = { showMessage(R.string.plan_error_update) }) {
            planRepository.removeExercise(planExerciseId)
            _events.send(PlanEvent.ExerciseRemoved(planExerciseId, name))
        }
    }

    /** Pairs the exercise with the one after it, so they are done in turn. */
    fun onSupersetWithNext(planExerciseId: String) {
        viewModelScope.launchCatching(onError = { showMessage(R.string.plan_error_update) }) {
            planRepository.supersetWithNext(planExerciseId)
        }
    }

    fun onRemoveFromSuperset(planExerciseId: String) {
        viewModelScope.launchCatching(onError = { showMessage(R.string.plan_error_update) }) {
            planRepository.removeFromSuperset(planExerciseId)
        }
    }

    fun onUndoRemoveExercise(planExerciseId: String) {
        viewModelScope.launchCatching(onError = { showMessage(R.string.plan_error_update) }) {
            planRepository.restoreExercise(planExerciseId)
        }
    }

    /** Adds a library exercise; [muscleGroup] is given when it had none yet. */
    fun onAddExercise(exerciseId: String, muscleGroup: MuscleGroup? = null) {
        viewModelScope.launchCatching(onError = { showMessage(R.string.plan_error_add_exercise) }) {
            if (muscleGroup != null) exerciseRepository.setMuscleGroup(exerciseId, muscleGroup)
            planRepository.addExercise(planId, exerciseId)
        }
    }

    /** Creates a new library exercise, with the muscle it trains, and adds it to this plan. */
    fun onCreateExercise(name: String, type: ExerciseType, muscleGroup: MuscleGroup) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launchCatching(onError = { showMessage(R.string.plan_error_add_exercise) }) {
            val exerciseId = exerciseRepository.createExercise(trimmed, type, muscleGroup)
            planRepository.addExercise(planId, exerciseId)
        }
    }

    fun onRenamePlan(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launchCatching(onError = { showMessage(R.string.plan_error_update) }) {
            planRepository.renamePlan(planId, trimmed)
        }
    }

    fun onDeletePlan() {
        viewModelScope.launchCatching(onError = { showMessage(R.string.plan_error_delete) }) {
            planRepository.deletePlan(planId)
            _events.send(PlanEvent.PlanDeleted)
        }
    }

    private suspend fun showMessage(@StringRes messageRes: Int) {
        _events.send(PlanEvent.ShowMessage(messageRes))
    }

    private fun observeContent(): Flow<PlanUiState> = combine(
        planRepository.observePlan(planId),
        planRepository.observePlans().map { plans -> plans.firstOrNull { it.id == planId } },
        workoutRepository.observeActiveWorkout(),
        exerciseRepository.observeExercises(),
        editing,
    ) { details, summary, active, library, edit ->
        mapper.map(details, summary, active, library, edit)
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as TrackerApplication).container
                PlanViewModel(
                    planId = createSavedStateHandle().toRoute<PlanDestination>().planId,
                    planRepository = container.planRepository,
                    workoutRepository = container.workoutRepository,
                    exerciseRepository = container.exerciseRepository,
                    time = container.time,
                )
            }
        }
    }
}
