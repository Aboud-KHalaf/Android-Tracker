package com.example.tracker.domain.reminder

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderTimeTest {

    private val zone = ZoneId.of("Europe/Berlin")

    private fun at(text: String): ZonedDateTime = LocalDateTime.parse(text).atZone(zone)

    @Test
    fun nextAfter_beforeNinePm_isToday() {
        assertEquals(at("2026-10-03T21:00"), ReminderTime.nextAfter(at("2026-10-03T08:30")))
    }

    @Test
    fun nextAfter_atOrAfterNinePm_isTomorrow() {
        assertEquals(at("2026-10-04T21:00"), ReminderTime.nextAfter(at("2026-10-03T21:00")))
        assertEquals(at("2026-10-04T21:00"), ReminderTime.nextAfter(at("2026-10-03T23:59")))
    }

    @Test
    fun nextAfter_acrossDaylightSavingChange_staysAtNinePmLocal() {
        // Berlin leaves summer time on 25 October 2026.
        assertEquals(at("2026-10-25T21:00"), ReminderTime.nextAfter(at("2026-10-24T22:00")))
    }
}
