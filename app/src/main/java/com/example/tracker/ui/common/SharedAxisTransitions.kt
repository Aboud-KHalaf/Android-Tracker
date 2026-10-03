package com.example.tracker.ui.common

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import com.example.tracker.ui.theme.Motion
import kotlin.math.roundToInt

/*
 * Material 3's shared X axis pattern, for moving between screens or steps in a sequence:
 * both sides slide a short way and crossfade, forward to the left and back to the right.
 */

/** How far content slides, as a fraction of its width. */
private const val SHARED_AXIS_OFFSET = 0.1f

private fun slideOffset(fullWidth: Int) = (fullWidth * SHARED_AXIS_OFFSET).roundToInt()

/** Content arriving: from the right going [forward], from the left going back. */
fun sharedAxisXIn(forward: Boolean): EnterTransition =
    slideInHorizontally(tween(Motion.DurationLong, easing = Motion.Emphasized)) {
        if (forward) slideOffset(it) else -slideOffset(it)
    } + fadeIn(tween(Motion.DurationMedium, delayMillis = Motion.DurationLong - Motion.DurationMedium, easing = Motion.EmphasizedDecelerate))

/** Content leaving: to the left going [forward], to the right going back. */
fun sharedAxisXOut(forward: Boolean): ExitTransition =
    slideOutHorizontally(tween(Motion.DurationLong, easing = Motion.Emphasized)) {
        if (forward) -slideOffset(it) else slideOffset(it)
    } + fadeOut(tween(Motion.DurationLong - Motion.DurationMedium, easing = Motion.EmphasizedAccelerate))

/** Both halves together, for `AnimatedContent`. */
fun sharedAxisX(forward: Boolean): ContentTransform = sharedAxisXIn(forward) togetherWith sharedAxisXOut(forward)
