package com.example.tracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.example.tracker.ui.exercise.exerciseDetailsScreen
import com.example.tracker.ui.exercise.navigateToExerciseDetails
import com.example.tracker.ui.home.HomeDestination
import com.example.tracker.ui.home.homeScreen
import com.example.tracker.ui.plan.navigateToPlan
import com.example.tracker.ui.plan.planScreen
import com.example.tracker.ui.workout.activeWorkoutScreen
import com.example.tracker.ui.workout.navigateToActiveWorkout

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
            onOpenWorkout = navController::navigateToActiveWorkout,
            onOpenPlan = navController::navigateToPlan,
            onOpenHistory = {}, // Wired when the History screen exists.
            onOpenExerciseProgress = navController::navigateToExerciseDetails,
        )
        planScreen(
            onBack = navController::popBackStack,
            onOpenWorkout = navController::navigateToActiveWorkout,
        )
        activeWorkoutScreen(
            onMinimize = navController::popBackStack,
            onWorkoutEnded = { navController.popBackStack(HomeDestination, inclusive = false) },
            onOpenExerciseProgress = navController::navigateToExerciseDetails,
        )
        exerciseDetailsScreen(onBack = navController::popBackStack)
    }
}
