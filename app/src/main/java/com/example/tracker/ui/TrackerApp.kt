package com.example.tracker.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.tracker.ui.navigation.TopLevelDestination
import com.example.tracker.ui.navigation.TrackerNavHost
import com.example.tracker.ui.navigation.TrackerNavigationBar
import com.example.tracker.ui.navigation.navigateToTopLevel

/**
 * App root: the navigation graph, with the bottom bar on the top-level screens only.
 * Screens below the bar see its height as already-consumed insets, so they don't pad twice.
 */
@Composable
fun TrackerApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentTab = TopLevelDestination.of(backStackEntry?.destination)

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (currentTab != null) {
                TrackerNavigationBar(selected = currentTab, onSelect = navController::navigateToTopLevel)
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
