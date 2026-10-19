package com.example.tracker.ui.history

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object HistoryDestination

fun NavController.navigateToHistory() = navigate(HistoryDestination) { launchSingleTop = true }

fun NavGraphBuilder.historyScreen(onBack: () -> Unit) {
    composable<HistoryDestination> { HistoryRoute(onBack = onBack) }
}
