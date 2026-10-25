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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * Progress tab: this week's working sets per muscle group, and the exercise library, each
 * opening its Exercise details. The library can be searched by name and filtered by muscle group.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ExerciseListViewModel(
    private val exerciseRepository: ExerciseRepository,
    private val workoutRepository: WorkoutRepository,
    private val time: TimeProvider,
) : ViewModel() {

    private val loadAttempt = MutableStateFlow(0)

    private val _query = MutableStateFlow("")

    /** The search text, kept apart from [uiState] so the field updates as the user types. */
    val query: StateFlow<String> = _query.asStateFlow()

    private val muscleFilter = MutableStateFlow<MuscleGroup?>(null)

    val uiState: StateFlow<ExerciseListUiState> = loadAttempt
        .flatMapLatest {
            // The weekly volume only changes with the data, not with every search keystroke.
            combine(exerciseRepository.observeExercises(), workoutRepository.observeAllSessions()) { exercises, sessions ->
                exercises.sortedBy { it.name.lowercase() } to muscleWeek(exercises, sessions)
            }
                .combine(combine(_query, muscleFilter, ::Pair)) { (exercises, muscleWeek), (query, filter) ->
                    ExerciseListUiState.Success(
                        exercises = exercises.matching(query, filter)
                            .map { ExerciseItemUi(it.id, it.name, it.type, it.muscleGroup) },
                        muscleWeek = muscleWeek,
                        hasExercises = exercises.isNotEmpty(),
                        muscleFilter = filter,
                        muscleOptions = MuscleGroup.entries.filter { group ->
                            group == filter || exercises.any { it.muscleGroup == group }
                        },
                    ) as ExerciseListUiState
                }
                .onStart { emit(ExerciseListUiState.Loading) }
                .catch { emit(ExerciseListUiState.Error) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ExerciseListUiState.Loading)

    fun onRetry() {
        loadAttempt.update { it + 1 }
    }

    fun onQueryChange(query: String) {
        _query.value = query
    }

    /** Shows only exercises training [group], or every exercise for null. */
    fun onMuscleFilterChange(group: MuscleGroup?) {
        muscleFilter.value = group
    }

    /** Clears the search and the muscle filter. */
    fun onClearFilters() {
        _query.value = ""
        muscleFilter.value = null
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

/**
 * The exercises whose name contains [query] (ignoring case and surrounding spaces) and, unless
 * [muscle] is null, that train [muscle].
 */
internal fun List<Exercise>.matching(query: String, muscle: MuscleGroup?): List<Exercise> {
    val text = query.trim()
    return filter { exercise ->
        (muscle == null || exercise.muscleGroup == muscle) && exercise.name.contains(text, ignoreCase = true)
    }
}
