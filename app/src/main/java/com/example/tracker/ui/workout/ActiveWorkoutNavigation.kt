package com.example.tracker.ui.workout

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data class ActiveWorkoutDestination(val workoutId: String)

fun NavController.navigateToActiveWorkout(workoutId: String) = navigate(ActiveWorkoutDestination(workoutId)) {
    // Opening the same workout again (e.g. Resume twice) shouldn't stack copies.
    launchSingleTop = true
}

fun NavGraphBuilder.activeWorkoutScreen(
    onMinimize: () -> Unit,
    onWorkoutEnded: () -> Unit,
    onOpenExerciseProgress: (exerciseId: String) -> Unit,
) {
    composable<ActiveWorkoutDestination> {
        ActiveWorkoutRoute(
            onMinimize = onMinimize,
            onWorkoutEnded = onWorkoutEnded,
            onOpenExerciseProgress = onOpenExerciseProgress,
        )
    }
}
