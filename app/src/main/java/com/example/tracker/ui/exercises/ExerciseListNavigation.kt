package com.example.tracker.ui.exercises

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object ExerciseListDestination

fun NavGraphBuilder.exerciseListScreen(onOpenExercise: (exerciseId: String) -> Unit) {
    composable<ExerciseListDestination> { ExerciseListRoute(onOpenExercise = onOpenExercise) }
}
