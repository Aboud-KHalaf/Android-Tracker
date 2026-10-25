package com.example.tracker.ui.exercises

import com.example.tracker.domain.model.ExerciseType
import com.example.tracker.domain.model.MuscleGroup

/** Everything the Progress tab (the exercise library) renders. */
sealed interface ExerciseListUiState {
    data object Loading : ExerciseListUiState
    data object Error : ExerciseListUiState

    /**
     * The exercises matching the search and [muscleFilter], sorted by name. [muscleWeek] is this
     * week's working sets per muscle group; empty when neither this week nor last had any.
     */
    data class Success(
        val exercises: List<ExerciseItemUi>,
        val muscleWeek: List<MuscleSetsUi> = emptyList(),
        /** Whether the library has any exercise, matching or not. */
        val hasExercises: Boolean = exercises.isNotEmpty(),
        /** Only exercises training this group are shown; null shows every exercise. */
        val muscleFilter: MuscleGroup? = null,
        /** The groups to filter by: those some exercise trains. */
        val muscleOptions: List<MuscleGroup> = emptyList(),
    ) : ExerciseListUiState
}

/** Working sets for one muscle group ([group] null: exercises without one). */
data class MuscleSetsUi(val group: MuscleGroup?, val thisWeek: Int, val lastWeek: Int)

data class ExerciseItemUi(val id: String, val name: String, val type: ExerciseType, val muscleGroup: MuscleGroup? = null)
