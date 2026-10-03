package com.example.tracker.ui

import androidx.compose.runtime.Composable
import com.example.tracker.ui.home.HomeRoute

/**
 * App root. Home is the only screen so far; the other destinations' callbacks get wired
 * up as those screens and navigation are added.
 */
@Composable
fun TrackerApp() {
    HomeRoute(
        onOpenWorkout = {},
        onOpenPlan = {},
        onOpenHistory = {},
        onOpenExerciseProgress = {},
    )
}
