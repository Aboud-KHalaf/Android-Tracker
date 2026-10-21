package com.example.tracker.domain.workout

import java.time.Instant

/** Tells the user a rest is over while the app is in the background. */
interface RestAlarm {
    /** Alerts the user at [endsAt] that the [kind] rest of [workoutId] is over. */
    fun schedule(workoutId: String, kind: RestKind, endsAt: Instant)

    /** Cancels a pending alert and removes one already shown. */
    fun cancel()
}
