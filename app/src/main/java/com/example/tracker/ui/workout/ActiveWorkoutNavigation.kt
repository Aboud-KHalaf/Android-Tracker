package com.example.tracker.ui.workout

import androidx.navigation.NavController
import kotlinx.serialization.Serializable

@Serializable
data class ActiveWorkoutDestination(val workoutId: String)

fun NavController.navigateToActiveWorkout(workoutId: String) = navigate(ActiveWorkoutDestination(workoutId)) {
    // Opening the same workout again (e.g. Resume twice) shouldn't stack copies.
    launchSingleTop = true
}
