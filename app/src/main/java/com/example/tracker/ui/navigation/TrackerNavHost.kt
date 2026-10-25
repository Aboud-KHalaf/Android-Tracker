package com.example.tracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.example.tracker.ui.exercise.exerciseDetailsScreen
import com.example.tracker.ui.exercise.navigateToExerciseDetails
import com.example.tracker.ui.exercises.exerciseListScreen
import com.example.tracker.ui.history.historyScreen
import com.example.tracker.ui.history.navigateToHistory
import com.example.tracker.ui.onboarding.onboardingScreen
import com.example.tracker.ui.summary.WorkoutSummaryDestination
import com.example.tracker.ui.summary.navigateToWorkoutSummary
import com.example.tracker.ui.summary.workoutSummaryScreen
import com.example.tracker.ui.home.HomeDestination
import com.example.tracker.ui.home.homeScreen
import com.example.tracker.ui.photos.navigateToPhotoCapture
import com.example.tracker.ui.photos.navigateToProgressPhotos
import com.example.tracker.ui.photos.photoCaptureScreen
import com.example.tracker.ui.photos.progressPhotosScreen
import com.example.tracker.ui.plan.navigateToPlan
import com.example.tracker.ui.plan.planScreen
import com.example.tracker.ui.program.navigateToProgram
import com.example.tracker.ui.program.programScreen
import com.example.tracker.ui.settings.navigateToSettings
import com.example.tracker.ui.settings.settingsScreen
import com.example.tracker.ui.stats.StatsDestination
import com.example.tracker.ui.stats.detail.StatDetailDestination
import com.example.tracker.ui.stats.detail.navigateToStatDetail
import com.example.tracker.ui.stats.detail.statDetailScreen
import com.example.tracker.ui.stats.editor.StatEditorDestination
import com.example.tracker.ui.stats.editor.navigateToEditStat
import com.example.tracker.ui.stats.editor.navigateToNewStat
import com.example.tracker.ui.stats.editor.statEditorScreen
import com.example.tracker.ui.stats.statsScreen
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
            onOpenProgram = navController::navigateToProgram,
            onOpenHistory = navController::navigateToHistory,
            onOpenExerciseProgress = navController::navigateToExerciseDetails,
            onOpenWorkoutSummary = navController::navigateToWorkoutSummary,
            onOpenSettings = navController::navigateToSettings,
        )
        historyScreen(onBack = navController::popBackStack, onOpenWorkout = navController::navigateToWorkoutSummary)
        workoutSummaryScreen(
            onBack = navController::popBackStack,
            onOpenExerciseProgress = navController::navigateToExerciseDetails,
        )
        onboardingScreen(
            // Onboarding sits over Home; a program made in it opens next, to add exercises.
            onFinished = { programId ->
                navController.popBackStack(HomeDestination, inclusive = false)
                programId?.let(navController::navigateToProgram)
            },
        )
        exerciseListScreen(
            onOpenExercise = navController::navigateToExerciseDetails,
            onOpenHistory = navController::navigateToHistory,
        )
        statsScreen(
            onOpenPhotos = navController::navigateToProgressPhotos,
            onTakePhotos = navController::navigateToPhotoCapture,
            onNewStat = navController::navigateToNewStat,
            onOpenStat = navController::navigateToStatDetail,
            onEditStat = navController::navigateToEditStat,
        )
        statDetailScreen(onBack = navController::popBackStack, onEdit = navController::navigateToEditStat)
        statEditorScreen(
            onClose = navController::popBackStack,
            // The new stat replaces its editor, so Back from it returns to Stats.
            onCreated = { statId ->
                navController.navigate(StatDetailDestination(statId)) {
                    popUpTo<StatEditorDestination> { inclusive = true }
                }
            },
            onDeleted = { navController.popBackStack(StatsDestination, inclusive = false) },
        )
        programScreen(onBack = navController::popBackStack, onOpenDay = navController::navigateToPlan)
        planScreen(
            onBack = navController::popBackStack,
            onOpenWorkout = navController::navigateToActiveWorkout,
        )
        activeWorkoutScreen(
            onMinimize = navController::popBackStack,
            onWorkoutEnded = { navController.popBackStack(HomeDestination, inclusive = false) },
            // The summary takes the workout's place, so Back from it returns Home.
            onWorkoutFinished = { workoutId ->
                navController.navigate(WorkoutSummaryDestination(workoutId, justFinished = true)) {
                    popUpTo(HomeDestination)
                }
            },
            onOpenExerciseProgress = navController::navigateToExerciseDetails,
        )
        exerciseDetailsScreen(onBack = navController::popBackStack)
        settingsScreen(onBack = navController::popBackStack)
        progressPhotosScreen(onBack = navController::popBackStack, onTakePhotos = navController::navigateToPhotoCapture)
        photoCaptureScreen(onClose = navController::popBackStack)
    }
}
