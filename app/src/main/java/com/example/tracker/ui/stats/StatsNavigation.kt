package com.example.tracker.ui.stats

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object StatsDestination

fun NavGraphBuilder.statsScreen(
    onOpenWeight: () -> Unit,
    onOpenDaily: () -> Unit,
    onOpenPhotos: () -> Unit,
    onTakePhotos: () -> Unit,
    onNewStat: (StatTemplate?) -> Unit,
    onOpenStat: (statId: String, logToday: Boolean) -> Unit,
) {
    composable<StatsDestination> {
        StatsRoute(
            onOpenWeight = onOpenWeight,
            onOpenDaily = onOpenDaily,
            onOpenPhotos = onOpenPhotos,
            onTakePhotos = onTakePhotos,
            onNewStat = onNewStat,
            onOpenStat = onOpenStat,
        )
    }
}
