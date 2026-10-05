package com.example.tracker.ui.plan

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable

@Serializable
data class PlanDestination(val planId: String)

fun NavController.navigateToPlan(planId: String) = navigate(PlanDestination(planId))

fun NavGraphBuilder.planScreen(
    onBack: () -> Unit,
    onOpenWorkout: (workoutId: String) -> Unit,
    onBrowseCatalog: (planId: String) -> Unit,
) {
    composable<PlanDestination> { backStackEntry ->
        val planId = backStackEntry.toRoute<PlanDestination>().planId
        PlanRoute(onBack = onBack, onOpenWorkout = onOpenWorkout, onBrowseCatalog = { onBrowseCatalog(planId) })
    }
}
