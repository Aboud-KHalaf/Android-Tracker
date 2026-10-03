package com.example.tracker.ui.exercise

import androidx.annotation.StringRes

/** One-off effects the Exercise details screen handles once. */
sealed interface ExerciseDetailsEvent {
    data class ShowMessage(@StringRes val messageRes: Int) : ExerciseDetailsEvent
}
