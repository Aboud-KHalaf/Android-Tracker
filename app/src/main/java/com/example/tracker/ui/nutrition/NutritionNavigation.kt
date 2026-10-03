package com.example.tracker.ui.nutrition

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object NutritionDestination

fun NavGraphBuilder.nutritionScreen() {
    composable<NutritionDestination> { NutritionRoute() }
}
