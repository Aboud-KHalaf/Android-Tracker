package com.example.tracker.ui.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import com.example.tracker.TrackerApplication
import com.example.tracker.core.TimeProvider
import com.example.tracker.domain.model.PersonalBest
import com.example.tracker.domain.model.Workout
import com.example.tracker.domain.repository.WorkoutRepository
import com.example.tracker.domain.workout.recapOf
import com.example.tracker.ui.common.SetValueUi
import com.example.tracker.ui.common.toSetValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * A finished workout at a glance: time, sets, volume against the previous workout of the same
 * training day, the personal bests it set, and each exercise's sets. State is exposed as [uiState].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutSummaryViewModel(
    private val workoutId: String,
    private val justFinished: Boolean,
    private val workoutRepository: WorkoutRepository,
    private val time: TimeProvider,
) : ViewModel() {

    private val loadAttempt = MutableStateFlow(0)

    val uiState: StateFlow<WorkoutSummaryUiState> = loadAttempt
        .flatMapLatest {
            workoutRepository.observeWorkout(workoutId)
                .flatMapLatest { workout ->
                    if (workout == null || workout.isInProgress) {
                        flowOf(WorkoutSummaryUiState.NotFound)
                    } else {
                        combine(workoutRepository.observePersonalBests(), observePrevious(workout)) { bests, previous ->
                            map(workout, bests, previous)
                        }
                    }
                }
                .onStart { emit(WorkoutSummaryUiState.Loading) }
                .catch { emit(WorkoutSummaryUiState.Error) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), WorkoutSummaryUiState.Loading)

    fun onRetry() {
        loadAttempt.update { it + 1 }
    }

    /** The workout of the same training day finished just before this one, if any. */
    private fun observePrevious(workout: Workout): Flow<Workout?> {
        val planId = workout.planId ?: return flowOf(null)
        val finishedAt = workout.finishedAt ?: return flowOf(null)
        return workoutRepository.observeHistory(planId)
            .map { history -> history.firstOrNull { it.finishedAt < finishedAt }?.id }
            .flatMapLatest { id -> if (id == null) flowOf(null) else workoutRepository.observeWorkout(id) }
    }

    private fun map(workout: Workout, bests: List<PersonalBest>, previous: Workout?): WorkoutSummaryUiState {
        val recap = recapOf(workout, bests, previous) ?: return WorkoutSummaryUiState.NotFound
        val names = recap.exercises.associate { it.exercise.id to it.exercise.name }
        val bestExercises = recap.personalBests.map { it.exerciseId }.toSet()
        return WorkoutSummaryUiState.Success(
            name = workout.name,
            date = workout.startedAt.atZone(time.zone()).toLocalDate(),
            justFinished = justFinished,
            duration = recap.duration,
            setCount = recap.completedSetCount,
            volumeKg = recap.volumeKg,
            volumeChangeKg = recap.volumeChangeKg,
            personalBests = recap.personalBests.mapNotNull { best ->
                val value = when {
                    best.weightKg != null && best.reps != null -> SetValueUi.WeightReps(best.weightKg, best.reps)
                    best.durationSeconds != null -> SetValueUi.Hold(best.durationSeconds)
                    else -> return@mapNotNull null
                }
                PersonalBestUi(names[best.exerciseId] ?: return@mapNotNull null, value)
            },
            exercises = recap.exercises.map { exercise ->
                ExerciseSummaryUi(
                    exerciseId = exercise.exercise.id,
                    name = exercise.exercise.name,
                    sets = exercise.sets.mapNotNull { it.toSetValue() },
                    isPersonalBest = exercise.exercise.id in bestExercises,
                )
            },
        )
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as TrackerApplication).container
                val destination = createSavedStateHandle().toRoute<WorkoutSummaryDestination>()
                WorkoutSummaryViewModel(
                    workoutId = destination.workoutId,
                    justFinished = destination.justFinished,
                    workoutRepository = container.workoutRepository,
                    time = container.time,
                )
            }
        }
    }
}
