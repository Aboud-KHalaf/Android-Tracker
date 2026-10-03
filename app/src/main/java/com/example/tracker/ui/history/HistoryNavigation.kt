package com.example.tracker.ui.history

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object HistoryDestination

fun NavGraphBuilder.historyScreen() {
    composable<HistoryDestination> { HistoryRoute() }
}
