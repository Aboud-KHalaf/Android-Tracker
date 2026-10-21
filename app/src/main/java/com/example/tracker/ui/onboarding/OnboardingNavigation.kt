package com.example.tracker.ui.onboarding

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object OnboardingDestination

/** Opens onboarding over Home, once. */
fun NavController.navigateToOnboarding() = navigate(OnboardingDestination) { launchSingleTop = true }

fun NavGraphBuilder.onboardingScreen(onFinished: (programId: String?) -> Unit) {
    composable<OnboardingDestination> { OnboardingRoute(onFinished = onFinished) }
}
