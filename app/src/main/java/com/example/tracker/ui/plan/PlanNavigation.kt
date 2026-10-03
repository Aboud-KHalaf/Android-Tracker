package com.example.tracker.ui.plan

import androidx.navigation.NavController
import kotlinx.serialization.Serializable

@Serializable
data class PlanDestination(val planId: String)

fun NavController.navigateToPlan(planId: String) = navigate(PlanDestination(planId))
