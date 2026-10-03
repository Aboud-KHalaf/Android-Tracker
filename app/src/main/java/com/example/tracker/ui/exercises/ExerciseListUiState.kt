package com.example.tracker.ui.exercises

import com.example.tracker.domain.model.ExerciseType

/** Everything the Progress tab (the exercise library) renders. */
sealed interface ExerciseListUiState {
    data object Loading : ExerciseListUiState
    data object Error : ExerciseListUiState

    /** Exercises sorted by name; empty before any exercise is created. */
    data class Success(val exercises: List<ExerciseItemUi>) : ExerciseListUiState
}

data class ExerciseItemUi(val id: String, val name: String, val type: ExerciseType)
