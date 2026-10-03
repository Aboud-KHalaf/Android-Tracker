package com.example.tracker.ui.plan

import com.example.tracker.domain.model.ExerciseType
import java.time.LocalDate

/** Everything the Workout plan screen renders. */
sealed interface PlanUiState {
    data object Loading : PlanUiState
    data object Error : PlanUiState

    /** The plan doesn't exist (any more), e.g. it was deleted. */
    data object NotFound : PlanUiState

    data class Success(
        val name: String,
        val lastDoneOn: LocalDate?,
        val exercises: List<PlanExerciseUi>,
        /** Library exercises that aren't in the plan yet, for the add-exercise sheet. */
        val availableExercises: List<ExerciseOptionUi>,
        val workoutAction: WorkoutActionUi,
        val isStartingWorkout: Boolean = false,
    ) : PlanUiState
}

data class PlanExerciseUi(
    /** The plan exercise's id (not the library exercise's). */
    val id: String,
    val name: String,
    val type: ExerciseType,
    val targetSets: Int,
    val canMoveUp: Boolean,
    val canMoveDown: Boolean,
)

data class ExerciseOptionUi(val id: String, val name: String, val type: ExerciseType)

/** What the screen's main button does. */
sealed interface WorkoutActionUi {
    /** Start a workout from this plan. */
    data object Start : WorkoutActionUi

    /** A workout (from this or another plan) is in progress; only one can run at a time. */
    data class Resume(val workoutId: String) : WorkoutActionUi

    /** The plan has no exercises, so there is nothing to start. */
    data object Unavailable : WorkoutActionUi
}
