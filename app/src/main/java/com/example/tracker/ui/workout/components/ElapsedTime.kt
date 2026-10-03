package com.example.tracker.ui.workout.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.delay

private const val SECOND_MILLIS = 1_000L

/**
 * The current time, refreshed every [intervalMillis] while [ticking]. Clocks on screen tick
 * here in the UI, so the screen state doesn't have to change every second.
 */
@Composable
fun rememberTickingNow(
    ticking: Boolean = true,
    intervalMillis: Long = SECOND_MILLIS,
    now: () -> Instant = Instant::now,
): Instant {
    val current by produceState(initialValue = now(), ticking, intervalMillis) {
        value = now()
        while (ticking) {
            delay(intervalMillis)
            value = now()
        }
    }
    return current
}

/** Seconds since [startedAt], ticking every second while on screen. */
@Composable
fun rememberElapsedSeconds(startedAt: Instant): Long =
    Duration.between(startedAt, rememberTickingNow()).seconds.coerceAtLeast(0)
