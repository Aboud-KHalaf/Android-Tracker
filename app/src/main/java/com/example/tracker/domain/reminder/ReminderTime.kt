package com.example.tracker.domain.reminder

import java.time.LocalTime
import java.time.ZonedDateTime

/** When the daily nutrition reminder goes off, in the user's time zone. */
object ReminderTime {
    val AT: LocalTime = LocalTime.of(21, 0)

    /** The next [AT] strictly after [now]: today if it hasn't passed yet, otherwise tomorrow. */
    fun nextAfter(now: ZonedDateTime): ZonedDateTime {
        val today = now.toLocalDate().atTime(AT).atZone(now.zone)
        return if (today.isAfter(now)) today else now.toLocalDate().plusDays(1).atTime(AT).atZone(now.zone)
    }
}
