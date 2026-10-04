package com.example.tracker.domain.reminder

import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderTimeTest {

    private val zone = ZoneId.of("Europe/Berlin")

    private fun at(text: String): ZonedDateTime = LocalDateTime.parse(text).atZone(zone)

    @Test
    fun nextAfter_beforeNinePm_isToday() {
        assertEquals(at("2026-10-03T21:00"), ReminderTime.nextAfter(at("2026-10-03T08:30"), ReminderTime.DEFAULT))
    }

    @Test
    fun nextAfter_atOrAfterNinePm_isTomorrow() {
        assertEquals(at("2026-10-04T21:00"), ReminderTime.nextAfter(at("2026-10-03T21:00"), ReminderTime.DEFAULT))
        assertEquals(at("2026-10-04T21:00"), ReminderTime.nextAfter(at("2026-10-03T23:59"), ReminderTime.DEFAULT))
    }

    @Test
    fun nextAfter_acrossDaylightSavingChange_staysAtNinePmLocal() {
        // Berlin leaves summer time on 25 October 2026.
        assertEquals(at("2026-10-25T21:00"), ReminderTime.nextAfter(at("2026-10-24T22:00"), ReminderTime.DEFAULT))
    }

    @Test
    fun nextAfter_customTime_usesIt() {
        val morning = LocalTime.of(7, 30)
        assertEquals(at("2026-10-03T07:30"), ReminderTime.nextAfter(at("2026-10-03T06:00"), morning))
        assertEquals(at("2026-10-04T07:30"), ReminderTime.nextAfter(at("2026-10-03T07:30"), morning))
    }
}
