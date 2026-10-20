package com.example.tracker.ui.stats

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import kotlinx.serialization.Serializable

@Serializable
data object StatsDestination

/** Opens the Stats tab, e.g. from the daily reminder notification. */
const val STATS_DEEP_LINK = "tracker://stats"

/** Where the reminder pointed before stats replaced the daily log; kept for notifications already shown. */
private const val LEGACY_DAILY_LOG_DEEP_LINK = "tracker://nutrition"

fun NavGraphBuilder.statsScreen(
    onOpenPhotos: () -> Unit,
    onTakePhotos: () -> Unit,
    onNewStat: (StatTemplate?) -> Unit,
    onOpenStat: (statId: String, logToday: Boolean) -> Unit,
) {
    composable<StatsDestination>(
        deepLinks = listOf(
            navDeepLink { uriPattern = STATS_DEEP_LINK },
            navDeepLink { uriPattern = LEGACY_DAILY_LOG_DEEP_LINK },
        ),
    ) {
        StatsRoute(
            onOpenPhotos = onOpenPhotos,
            onTakePhotos = onTakePhotos,
            onNewStat = onNewStat,
            onOpenStat = onOpenStat,
        )
    }
}
