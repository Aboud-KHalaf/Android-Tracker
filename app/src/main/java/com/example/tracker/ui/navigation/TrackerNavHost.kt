package com.example.tracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.example.tracker.ui.home.HomeDestination
import com.example.tracker.ui.home.homeScreen
import com.example.tracker.ui.plan.navigateToPlan
import com.example.tracker.ui.plan.planScreen

/**
 * The app's navigation graph. Each screen registers itself through its own
 * `NavGraphBuilder` extension; this file only connects them.
 */
@Composable
fun TrackerNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(navController = navController, startDestination = HomeDestination, modifier = modifier) {
        homeScreen(
            onOpenWorkout = {}, // Wired when the Active workout screen exists.
            onOpenPlan = navController::navigateToPlan,
            onOpenHistory = {}, // Wired when the History screen exists.
            onOpenExerciseProgress = {}, // Wired when the Exercise details screen exists.
        )
        planScreen(
            onBack = navController::popBackStack,
            onOpenWorkout = {}, // Wired when the Active workout screen exists.
        )
    }
}
