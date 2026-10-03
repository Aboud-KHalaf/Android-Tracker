package com.example.tracker.ui.workout

import androidx.annotation.StringRes

/** One-off effects the Active workout screen handles once. */
sealed interface ActiveWorkoutEvent {
    /** A set was completed; the screen offers to undo it. [setNumber] is one-based. */
    data class SetCompleted(val setId: String, val setNumber: Int) : ActiveWorkoutEvent

    /** The workout was finished or discarded; leave the screen. */
    data object WorkoutEnded : ActiveWorkoutEvent

    data class ShowMessage(@StringRes val messageRes: Int) : ActiveWorkoutEvent
}
