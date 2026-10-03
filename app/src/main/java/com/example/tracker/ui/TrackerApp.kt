package com.example.tracker.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.tracker.ui.navigation.TopLevelDestination
import com.example.tracker.ui.navigation.TrackerNavHost
import com.example.tracker.ui.navigation.TrackerNavigationBar
import com.example.tracker.ui.navigation.navigateToTopLevel
import com.example.tracker.ui.theme.Motion

/**
 * App root: the navigation graph, with the bottom bar on the top-level screens only.
 * Screens below the bar see its height as already-consumed insets, so they don't pad twice.
 */
@Composable
fun TrackerApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentTab = TopLevelDestination.of(backStackEntry?.destination)

    // While the bar slides away it keeps showing the tab it was on.
    var lastTab by rememberSaveable { mutableStateOf(TopLevelDestination.HOME) }
    if (currentTab != null && currentTab != lastTab) lastTab = currentTab

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            AnimatedVisibility(
                visible = currentTab != null,
                enter = expandVertically(tween(Motion.DurationLong, easing = Motion.Emphasized), expandFrom = Alignment.Top),
                exit = shrinkVertically(tween(Motion.DurationLong, easing = Motion.Emphasized), shrinkTowards = Alignment.Top),
            ) {
                TrackerNavigationBar(selected = currentTab ?: lastTab, onSelect = navController::navigateToTopLevel)
            }
        },
    ) { innerPadding ->
        TrackerNavHost(
            navController = navController,
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        )
    }
}
