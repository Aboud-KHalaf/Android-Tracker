package com.example.tracker.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing

/**
 * Material 3 motion tokens: how long transitions take and how they accelerate. Use these
 * instead of literal durations so the app moves consistently.
 * See https://m3.material.io/styles/motion/easing-and-duration/tokens-specs.
 */
object Motion {
    /** Small changes: a selection, a check mark, a fade. */
    const val DurationShort = 200

    /** Most component changes: expanding, crossfading content, a value changing. */
    const val DurationMedium = 300

    /** Screen transitions and larger elements entering. */
    const val DurationLong = 400

    /** Drawing data in, e.g. a chart's line or bars. */
    const val DurationExtraLong = 700

    /** Elements that start and end on screen. */
    val Emphasized: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** Elements entering the screen: fast start, gentle landing. */
    val EmphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    /** Elements leaving the screen for good. */
    val EmphasizedAccelerate: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
}
