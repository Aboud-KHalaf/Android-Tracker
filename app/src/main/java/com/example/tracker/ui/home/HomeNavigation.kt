package com.example.tracker.ui.home

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object HomeDestination

fun NavGraphBuilder.homeScreen(
    onOpenWorkout: (workoutId: String) -> Unit,
    onOpenProgram: (programId: String) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenExerciseProgress: (exerciseId: String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenWorkoutSummary: (workoutId: String) -> Unit = {},
) {
    composable<HomeDestination> {
        HomeRoute(
            onOpenWorkout = onOpenWorkout,
            onOpenProgram = onOpenProgram,
            onOpenHistory = onOpenHistory,
            onOpenExerciseProgress = onOpenExerciseProgress,
            onOpenSettings = onOpenSettings,
            onOpenWorkoutSummary = onOpenWorkoutSummary,
        )
    }
}
