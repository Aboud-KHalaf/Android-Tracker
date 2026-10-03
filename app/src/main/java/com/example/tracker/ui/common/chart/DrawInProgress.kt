package com.example.tracker.ui.common.chart

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import com.example.tracker.ui.theme.Motion

/**
 * How far a chart has drawn in, from 0 to 1. It plays when the chart first appears and
 * again whenever [data] changes, e.g. after picking another time range. Read it in a draw
 * phase so that only drawing repeats, not composition.
 */
@Composable
fun rememberDrawInProgress(data: Any): State<Float> {
    val progress = remember(data) { Animatable(0f) }
    LaunchedEffect(progress) {
        progress.animateTo(1f, tween(Motion.DurationExtraLong, easing = Motion.Emphasized))
    }
    return progress.asState()
}

/**
 * The part of a staggered animation that belongs to item [index] of [count]: items start
 * one after another, each taking the share of [progress] that [stagger] leaves.
 */
fun staggered(progress: Float, index: Int, count: Int, stagger: Float = 0.5f): Float {
    if (count <= 1) return progress
    val start = stagger * index / (count - 1)
    return ((progress - start) / (1f - stagger)).coerceIn(0f, 1f)
}
