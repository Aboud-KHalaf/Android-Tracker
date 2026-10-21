package com.example.tracker.ui.summary

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

/** [justFinished] is true when the workout was finished a moment ago, rather than opened from a list. */
@Serializable
data class WorkoutSummaryDestination(val workoutId: String, val justFinished: Boolean = false)

fun NavController.navigateToWorkoutSummary(workoutId: String) = navigate(WorkoutSummaryDestination(workoutId)) {
    launchSingleTop = true
}

fun NavGraphBuilder.workoutSummaryScreen(
    onBack: () -> Unit,
    onOpenExerciseProgress: (exerciseId: String) -> Unit,
) {
    composable<WorkoutSummaryDestination> {
        WorkoutSummaryRoute(onBack = onBack, onOpenExerciseProgress = onOpenExerciseProgress)
    }
}
