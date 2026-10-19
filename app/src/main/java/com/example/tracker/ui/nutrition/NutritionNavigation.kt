package com.example.tracker.ui.nutrition

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import kotlinx.serialization.Serializable

@Serializable
data object NutritionDestination

/** Opens the daily log, e.g. from the daily reminder notification. */
const val NUTRITION_DEEP_LINK = "tracker://nutrition"

fun NavController.navigateToNutrition() = navigate(NutritionDestination) { launchSingleTop = true }

fun NavGraphBuilder.nutritionScreen(onBack: () -> Unit) {
    composable<NutritionDestination>(
        deepLinks = listOf(navDeepLink { uriPattern = NUTRITION_DEEP_LINK }),
    ) { NutritionRoute(onBack = onBack) }
}
