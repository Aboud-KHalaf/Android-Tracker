package com.example.tracker.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.example.tracker.ui.theme.Motion

/** Grows a floating action button in when it first appears or becomes [visible], and shrinks it away. */
@Composable
fun AnimatedFab(visible: Boolean = true, content: @Composable () -> Unit) {
    val state = remember { MutableTransitionState(false) }.apply { targetState = visible }
    AnimatedVisibility(
        visibleState = state,
        enter = scaleIn(tween(Motion.DurationMedium, easing = Motion.EmphasizedDecelerate)) +
            fadeIn(tween(Motion.DurationShort)),
        exit = scaleOut(tween(Motion.DurationShort, easing = Motion.EmphasizedAccelerate)) +
            fadeOut(tween(Motion.DurationShort)),
    ) { content() }
}
