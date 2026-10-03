package com.example.tracker.ui.exercise

import androidx.navigation.NavController
import kotlinx.serialization.Serializable

@Serializable
data class ExerciseDetailsDestination(val exerciseId: String)

fun NavController.navigateToExerciseDetails(exerciseId: String) = navigate(ExerciseDetailsDestination(exerciseId)) {
    launchSingleTop = true
}
