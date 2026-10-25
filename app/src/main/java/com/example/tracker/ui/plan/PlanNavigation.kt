package com.example.tracker.ui.plan

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data class PlanDestination(val planId: String)

fun NavController.navigateToPlan(planId: String) = navigate(PlanDestination(planId))

fun NavGraphBuilder.planScreen(
    onBack: () -> Unit,
    onOpenWorkout: (workoutId: String) -> Unit,
) {
    composable<PlanDestination> {
        PlanRoute(onBack = onBack, onOpenWorkout = onOpenWorkout)
    }
}
