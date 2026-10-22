package com.example.tracker.ui.exercises

import com.example.tracker.domain.model.ExerciseType
import com.example.tracker.domain.model.MuscleGroup

/** Everything the Progress tab (the exercise library) renders. */
sealed interface ExerciseListUiState {
    data object Loading : ExerciseListUiState
    data object Error : ExerciseListUiState

    /**
     * Exercises sorted by name; empty before any exercise is created. [muscleWeek] is this
     * week's working sets per muscle group; empty when neither this week nor last had any.
     */
    data class Success(
        val exercises: List<ExerciseItemUi>,
        val muscleWeek: List<MuscleSetsUi> = emptyList(),
    ) : ExerciseListUiState
}

/** Working sets for one muscle group ([group] null: exercises without one). */
data class MuscleSetsUi(val group: MuscleGroup?, val thisWeek: Int, val lastWeek: Int)

data class ExerciseItemUi(val id: String, val name: String, val type: ExerciseType)
