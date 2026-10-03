package com.example.tracker.ui.settings

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object SettingsDestination

fun NavController.navigateToSettings() = navigate(SettingsDestination)

fun NavGraphBuilder.settingsScreen(onBack: () -> Unit) {
    composable<SettingsDestination> {
        SettingsRoute(onBack = onBack)
    }
}
