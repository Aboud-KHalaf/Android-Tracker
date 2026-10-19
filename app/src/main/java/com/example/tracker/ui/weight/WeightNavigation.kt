package com.example.tracker.ui.weight

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object WeightDestination

fun NavController.navigateToWeight() = navigate(WeightDestination) { launchSingleTop = true }

fun NavGraphBuilder.weightScreen(onBack: () -> Unit) {
    composable<WeightDestination> { WeightRoute(onBack = onBack) }
}
