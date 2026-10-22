package com.example.tracker.ui.plan

import com.example.tracker.domain.model.ExerciseConfig
import com.example.tracker.domain.model.ExerciseType
import com.example.tracker.ui.common.SetValueUi
import java.time.DayOfWeek
import java.time.LocalDate

/** Everything the training day screen renders. */
sealed interface PlanUiState {
    data object Loading : PlanUiState
    data object Error : PlanUiState

    /** The day doesn't exist (any more), e.g. it was deleted. */
    data object NotFound : PlanUiState

    data class Success(
        val name: String,
        /** The weekday it is scheduled on, or null for "any day". */
        val dayOfWeek: DayOfWeek?,
        val lastDoneOn: LocalDate?,
        val exercises: List<PlanExerciseUi>,
        /** Library exercises that aren't in the day yet, to add or to replace one with. */
        val availableExercises: List<ExerciseOptionUi>,
        val workoutAction: WorkoutActionUi,
        val isStartingWorkout: Boolean = false,
        /** The exercise whose targets are being edited, if any. */
        val editor: ExerciseEditorUi? = null,
    ) : PlanUiState
}

data class PlanExerciseUi(
    /** The plan exercise's id. */
    val id: String,
    /** The library exercise's id. */
    val exerciseId: String,
    val name: String,
    val type: ExerciseType,
    val config: ExerciseConfig,
    val canMoveUp: Boolean,
    val canMoveDown: Boolean,
    /** Its superset's letter ("A" for the day's first superset), or null when it isn't in one. */
    val supersetLabel: String? = null,
) {
    /** The last exercise has nothing after it to pair with. */
    val canSupersetWithNext: Boolean get() = canMoveDown
}

data class ExerciseOptionUi(val id: String, val name: String, val type: ExerciseType)

/**
 * The "Edit targets" sheet: the exercise's default configuration and, kept apart from it,
 * what was actually recorded the last time it was done.
 */
data class ExerciseEditorUi(
    val planExerciseId: String,
    val name: String,
    val type: ExerciseType,
    val config: ExerciseConfig,
    /** Sets from the last finished workout with this exercise; empty if never done. */
    val lastRecorded: List<SetValueUi>,
    val isSaving: Boolean = false,
)

/** What the screen's main button does. */
sealed interface WorkoutActionUi {
    /** Start a workout from this day. */
    data object Start : WorkoutActionUi

    /** A workout (from this or another day) is in progress; only one can run at a time. */
    data class Resume(val workoutId: String) : WorkoutActionUi

    /** The day has no exercises, so there is nothing to start. */
    data object Unavailable : WorkoutActionUi
}
