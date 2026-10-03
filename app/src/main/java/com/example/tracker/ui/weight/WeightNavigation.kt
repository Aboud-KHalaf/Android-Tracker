package com.example.tracker.ui.weight

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object WeightDestination

fun NavGraphBuilder.weightScreen() {
    composable<WeightDestination> { WeightRoute() }
}
