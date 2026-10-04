package com.example.tracker.domain.reminder

import java.time.LocalTime
import java.time.ZonedDateTime

/** When the daily nutrition reminder goes off, in the user's time zone. */
object ReminderTime {
    val DEFAULT: LocalTime = LocalTime.of(21, 0)

    /** The next [at] strictly after [now]: today if it hasn't passed yet, otherwise tomorrow. */
    fun nextAfter(now: ZonedDateTime, at: LocalTime): ZonedDateTime {
        val today = now.toLocalDate().atTime(at).atZone(now.zone)
        return if (today.isAfter(now)) today else now.toLocalDate().plusDays(1).atTime(at).atZone(now.zone)
    }
}
