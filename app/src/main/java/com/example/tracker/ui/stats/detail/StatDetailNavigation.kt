package com.example.tracker.ui.stats.detail

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

/** One custom stat; [logToday] opens today's entry as soon as the screen opens. */
@Serializable
data class StatDetailDestination(val statId: String, val logToday: Boolean = false)

fun NavController.navigateToStatDetail(statId: String, logToday: Boolean = false) =
    navigate(StatDetailDestination(statId, logToday)) { launchSingleTop = true }

fun NavGraphBuilder.statDetailScreen(onBack: () -> Unit, onEdit: (statId: String) -> Unit) {
    composable<StatDetailDestination> { StatDetailRoute(onBack = onBack, onEdit = onEdit) }
}
