package com.example.tracker.ui.exercises

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.tracker.TrackerApplication
import com.example.tracker.core.TimeProvider
import com.example.tracker.domain.model.Exercise
import com.example.tracker.domain.model.ExerciseSession
import com.example.tracker.domain.model.MuscleGroup
import com.example.tracker.domain.progress.setsPerMuscleGroup
import com.example.tracker.domain.repository.ExerciseRepository
import com.example.tracker.domain.repository.WorkoutRepository
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * Progress tab: this week's working sets per muscle group, and the exercise library, each
 * opening its Exercise details.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ExerciseListViewModel(
    private val exerciseRepository: ExerciseRepository,
    private val workoutRepository: WorkoutRepository,
    private val time: TimeProvider,
) : ViewModel() {

    private val loadAttempt = MutableStateFlow(0)

    val uiState: StateFlow<ExerciseListUiState> = loadAttempt
        .flatMapLatest {
            combine(exerciseRepository.observeExercises(), workoutRepository.observeAllSessions()) { exercises, sessions ->
                ExerciseListUiState.Success(
                    exercises = exercises
                        .sortedBy { it.name.lowercase() }
                        .map { ExerciseItemUi(it.id, it.name, it.type) },
                    muscleWeek = muscleWeek(exercises, sessions),
                ) as ExerciseListUiState
            }
                .onStart { emit(ExerciseListUiState.Loading) }
                .catch { emit(ExerciseListUiState.Error) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ExerciseListUiState.Loading)

    fun onRetry() {
        loadAttempt.update { it + 1 }
    }

    /** Every muscle group (and "none" when it has sets), this week against last. */
    private fun muscleWeek(exercises: List<Exercise>, sessions: List<ExerciseSession>): List<MuscleSetsUi> {
        val zone = time.zone()
        val thisWeekStart = time.now().atZone(zone).toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        fun startOf(week: LocalDate) = week.atStartOfDay(zone).toInstant()
        val thisWeek = setsPerMuscleGroup(sessions, exercises, startOf(thisWeekStart), startOf(thisWeekStart.plusWeeks(1)))
        val lastWeek = setsPerMuscleGroup(sessions, exercises, startOf(thisWeekStart.minusWeeks(1)), startOf(thisWeekStart))
        if (thisWeek.isEmpty() && lastWeek.isEmpty()) return emptyList()
        return (MuscleGroup.entries + null)
            .map { group -> MuscleSetsUi(group, thisWeek[group] ?: 0, lastWeek[group] ?: 0) }
            .filter { it.group != null || it.thisWeek > 0 || it.lastWeek > 0 }
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as TrackerApplication).container
                ExerciseListViewModel(container.exerciseRepository, container.workoutRepository, container.time)
            }
        }
    }
}
