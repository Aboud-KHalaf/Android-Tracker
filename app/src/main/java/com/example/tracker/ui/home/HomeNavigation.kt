package com.example.tracker.ui.home

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object HomeDestination

fun NavGraphBuilder.homeScreen(
    onOpenWorkout: (workoutId: String) -> Unit,
    onOpenPlan: (planId: String) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenExerciseProgress: (exerciseId: String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    composable<HomeDestination> {
        HomeRoute(
            onOpenWorkout = onOpenWorkout,
            onOpenPlan = onOpenPlan,
            onOpenHistory = onOpenHistory,
            onOpenExerciseProgress = onOpenExerciseProgress,
            onOpenSettings = onOpenSettings,
        )
    }
}
