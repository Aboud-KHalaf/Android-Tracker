package com.example.tracker.ui.workout.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.delay

private const val TICK_MILLIS = 1_000L

/**
 * Seconds since [startedAt], ticking every second while on screen. Kept in the UI so the
 * whole screen state doesn't change every second.
 */
@Composable
fun rememberElapsedSeconds(startedAt: Instant, now: () -> Instant = Instant::now): Long {
    val elapsed by produceState(initialValue = secondsSince(startedAt, now()), startedAt) {
        while (true) {
            value = secondsSince(startedAt, now())
            delay(TICK_MILLIS)
        }
    }
    return elapsed
}

private fun secondsSince(start: Instant, now: Instant): Long = Duration.between(start, now).seconds.coerceAtLeast(0)
