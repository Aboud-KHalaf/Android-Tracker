package com.example.tracker.ui.catalog

import androidx.annotation.StringRes

/** One-off effects the Exercise catalog screen handles once. */
sealed interface ExerciseCatalogEvent {
    data class ExerciseAdded(val exerciseName: String) : ExerciseCatalogEvent

    data class ShowMessage(@StringRes val messageRes: Int) : ExerciseCatalogEvent
}
