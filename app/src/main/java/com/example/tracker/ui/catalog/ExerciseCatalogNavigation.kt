package com.example.tracker.ui.catalog

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

/** Browsing the online catalog to add exercises to the plan [planId]. */
@Serializable
data class ExerciseCatalogDestination(val planId: String)

fun NavController.navigateToExerciseCatalog(planId: String) = navigate(ExerciseCatalogDestination(planId))

fun NavGraphBuilder.exerciseCatalogScreen(onBack: () -> Unit) {
    composable<ExerciseCatalogDestination> {
        ExerciseCatalogRoute(onBack = onBack)
    }
}
