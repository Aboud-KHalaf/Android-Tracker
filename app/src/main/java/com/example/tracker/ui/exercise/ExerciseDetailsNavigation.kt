package com.example.tracker.ui.exercise

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data class ExerciseDetailsDestination(val exerciseId: String)

fun NavController.navigateToExerciseDetails(exerciseId: String) = navigate(ExerciseDetailsDestination(exerciseId)) {
    launchSingleTop = true
}

fun NavGraphBuilder.exerciseDetailsScreen(onBack: () -> Unit) {
    composable<ExerciseDetailsDestination> {
        ExerciseDetailsRoute(onBack = onBack)
    }
}
