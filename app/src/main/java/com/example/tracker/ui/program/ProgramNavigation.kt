package com.example.tracker.ui.program

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data class ProgramDestination(val programId: String)

fun NavController.navigateToProgram(programId: String) = navigate(ProgramDestination(programId))

fun NavGraphBuilder.programScreen(
    onBack: () -> Unit,
    onOpenDay: (planId: String) -> Unit,
) {
    composable<ProgramDestination> {
        ProgramRoute(onBack = onBack, onOpenDay = onOpenDay)
    }
}
