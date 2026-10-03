package com.example.tracker.ui.home

import androidx.annotation.StringRes

/** One-off effects the Home screen handles once: navigation and messages. */
sealed interface HomeEvent {
    data class OpenWorkout(val workoutId: String) : HomeEvent
    data class OpenPlan(val planId: String) : HomeEvent
    data class ShowMessage(@StringRes val messageRes: Int) : HomeEvent
}
