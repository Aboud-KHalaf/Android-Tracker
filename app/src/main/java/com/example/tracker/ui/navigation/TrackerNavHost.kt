package com.example.tracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.example.tracker.ui.catalog.exerciseCatalogScreen
import com.example.tracker.ui.catalog.navigateToExerciseCatalog
import com.example.tracker.ui.exercise.exerciseDetailsScreen
import com.example.tracker.ui.exercise.navigateToExerciseDetails
import com.example.tracker.ui.exercises.exerciseListScreen
import com.example.tracker.ui.history.historyScreen
import com.example.tracker.ui.home.HomeDestination
import com.example.tracker.ui.home.homeScreen
import com.example.tracker.ui.nutrition.nutritionScreen
import com.example.tracker.ui.plan.navigateToPlan
import com.example.tracker.ui.plan.planScreen
import com.example.tracker.ui.settings.navigateToSettings
import com.example.tracker.ui.settings.settingsScreen
import com.example.tracker.ui.weight.weightScreen
import com.example.tracker.ui.workout.activeWorkoutScreen
import com.example.tracker.ui.workout.navigateToActiveWorkout

/**
 * The app's navigation graph. Each screen registers itself through its own
 * `NavGraphBuilder` extension; this file only connects them.
 */
@Composable
fun TrackerNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = HomeDestination,
        modifier = modifier,
        enterTransition = { enterTransition() },
        exitTransition = { exitTransition() },
        popEnterTransition = { popEnterTransition() },
        popExitTransition = { popExitTransition() },
    ) {
        homeScreen(
            onOpenWorkout = navController::navigateToActiveWorkout,
            onOpenPlan = navController::navigateToPlan,
            onOpenHistory = { navController.navigateToTopLevel(TopLevelDestination.HISTORY) },
            onOpenExerciseProgress = navController::navigateToExerciseDetails,
            onOpenSettings = navController::navigateToSettings,
        )
        historyScreen()
        exerciseListScreen(onOpenExercise = navController::navigateToExerciseDetails)
        nutritionScreen()
        weightScreen()
        planScreen(
            onBack = navController::popBackStack,
            onOpenWorkout = navController::navigateToActiveWorkout,
            onBrowseCatalog = navController::navigateToExerciseCatalog,
        )
        activeWorkoutScreen(
            onMinimize = navController::popBackStack,
            onWorkoutEnded = { navController.popBackStack(HomeDestination, inclusive = false) },
            onOpenExerciseProgress = navController::navigateToExerciseDetails,
        )
        exerciseCatalogScreen(onBack = navController::popBackStack)
        exerciseDetailsScreen(onBack = navController::popBackStack)
        settingsScreen(onBack = navController::popBackStack)
    }
}
