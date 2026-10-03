package com.example.tracker.ui.plan

import androidx.annotation.StringRes

/** One-off effects the Workout plan screen handles once. */
sealed interface PlanEvent {
    data class OpenWorkout(val workoutId: String) : PlanEvent

    /** An exercise was removed; the screen offers to undo it. */
    data class ExerciseRemoved(val planExerciseId: String, val exerciseName: String) : PlanEvent

    /** The plan was deleted; leave the screen. */
    data object PlanDeleted : PlanEvent

    data class ShowMessage(@StringRes val messageRes: Int) : PlanEvent
}
