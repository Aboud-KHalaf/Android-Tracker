package com.example.tracker.ui.nutrition

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import kotlinx.serialization.Serializable

@Serializable
data object NutritionDestination

/** Opens the Nutrition tab, e.g. from the daily reminder notification. */
const val NUTRITION_DEEP_LINK = "tracker://nutrition"

fun NavGraphBuilder.nutritionScreen() {
    composable<NutritionDestination>(
        deepLinks = listOf(navDeepLink { uriPattern = NUTRITION_DEEP_LINK }),
    ) { NutritionRoute() }
}
