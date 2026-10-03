package com.example.tracker.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.navigation.NavBackStackEntry
import com.example.tracker.ui.common.sharedAxisXIn
import com.example.tracker.ui.common.sharedAxisXOut
import com.example.tracker.ui.theme.Motion

/*
 * Screen transitions, following Material 3's transition patterns:
 * - Switching tabs fades through: the old tab fades out, then the new one fades and zooms in
 *   slightly. The tabs are peers, so neither slides over the other.
 * - Opening a screen and going back use the shared X axis (see sharedAxisXIn).
 */

private typealias Transitions = AnimatedContentTransitionScope<NavBackStackEntry>

/** Where a tab starts zooming in from. */
private const val FADE_THROUGH_SCALE = 0.94f

/** The old tab fades out in the first part of the transition, the new one fades in after it. */
private const val FADE_THROUGH_OUT_DURATION = Motion.DurationMedium * 3 / 10

private val Transitions.isTabSwitch: Boolean
    get() = TopLevelDestination.of(initialState.destination) != null &&
        TopLevelDestination.of(targetState.destination) != null

private fun fadeThroughIn(): EnterTransition =
    fadeIn(tween(Motion.DurationMedium - FADE_THROUGH_OUT_DURATION, delayMillis = FADE_THROUGH_OUT_DURATION, easing = Motion.EmphasizedDecelerate)) +
        scaleIn(
            animationSpec = tween(Motion.DurationMedium, easing = Motion.EmphasizedDecelerate),
            initialScale = FADE_THROUGH_SCALE,
        )

private fun fadeThroughOut(): ExitTransition =
    fadeOut(tween(FADE_THROUGH_OUT_DURATION, easing = Motion.EmphasizedAccelerate))

internal fun Transitions.enterTransition(): EnterTransition =
    if (isTabSwitch) fadeThroughIn() else sharedAxisXIn(forward = true)

internal fun Transitions.exitTransition(): ExitTransition =
    if (isTabSwitch) fadeThroughOut() else sharedAxisXOut(forward = true)

internal fun Transitions.popEnterTransition(): EnterTransition =
    if (isTabSwitch) fadeThroughIn() else sharedAxisXIn(forward = false)

internal fun Transitions.popExitTransition(): ExitTransition =
    if (isTabSwitch) fadeThroughOut() else sharedAxisXOut(forward = false)
