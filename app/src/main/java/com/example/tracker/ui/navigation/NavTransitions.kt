package com.example.tracker.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavBackStackEntry
import com.example.tracker.ui.theme.Motion
import kotlin.math.roundToInt

/*
 * Screen transitions, following Material 3's transition patterns:
 * - Switching tabs fades through: the old tab fades out, then the new one fades and zooms in
 *   slightly. The tabs are peers, so neither slides over the other.
 * - Opening a screen and going back use the shared X axis: both screens slide a short way and
 *   crossfade, forward to the left and back to the right.
 */

private typealias Transitions = AnimatedContentTransitionScope<NavBackStackEntry>

/** How far the screens slide on the shared axis, as a fraction of the screen width. */
private const val SHARED_AXIS_OFFSET = 0.1f

/** Where a tab starts zooming in from. */
private const val FADE_THROUGH_SCALE = 0.94f

/** The old tab fades out in the first part of the transition, the new one fades in after it. */
private const val FADE_THROUGH_OUT_DURATION = Motion.DurationMedium * 3 / 10

private val Transitions.isTabSwitch: Boolean
    get() = TopLevelDestination.of(initialState.destination) != null &&
        TopLevelDestination.of(targetState.destination) != null

private fun slideOffset(fullWidth: Int) = (fullWidth * SHARED_AXIS_OFFSET).roundToInt()

private fun fadeThroughIn(): EnterTransition =
    fadeIn(tween(Motion.DurationMedium - FADE_THROUGH_OUT_DURATION, delayMillis = FADE_THROUGH_OUT_DURATION, easing = Motion.EmphasizedDecelerate)) +
        scaleIn(
            animationSpec = tween(Motion.DurationMedium, easing = Motion.EmphasizedDecelerate),
            initialScale = FADE_THROUGH_SCALE,
        )

private fun fadeThroughOut(): ExitTransition =
    fadeOut(tween(FADE_THROUGH_OUT_DURATION, easing = Motion.EmphasizedAccelerate))

/** A screen arriving on the shared X axis: from the right going forward, from the left going back. */
private fun sharedAxisIn(forward: Boolean): EnterTransition =
    slideInHorizontally(tween(Motion.DurationLong, easing = Motion.Emphasized)) {
        if (forward) slideOffset(it) else -slideOffset(it)
    } + fadeIn(tween(Motion.DurationMedium, delayMillis = Motion.DurationLong - Motion.DurationMedium, easing = Motion.EmphasizedDecelerate))

private fun sharedAxisOut(forward: Boolean): ExitTransition =
    slideOutHorizontally(tween(Motion.DurationLong, easing = Motion.Emphasized)) {
        if (forward) -slideOffset(it) else slideOffset(it)
    } + fadeOut(tween(Motion.DurationLong - Motion.DurationMedium, easing = Motion.EmphasizedAccelerate))

internal fun Transitions.enterTransition(): EnterTransition =
    if (isTabSwitch) fadeThroughIn() else sharedAxisIn(forward = true)

internal fun Transitions.exitTransition(): ExitTransition =
    if (isTabSwitch) fadeThroughOut() else sharedAxisOut(forward = true)

internal fun Transitions.popEnterTransition(): EnterTransition =
    if (isTabSwitch) fadeThroughIn() else sharedAxisIn(forward = false)

internal fun Transitions.popExitTransition(): ExitTransition =
    if (isTabSwitch) fadeThroughOut() else sharedAxisOut(forward = false)
