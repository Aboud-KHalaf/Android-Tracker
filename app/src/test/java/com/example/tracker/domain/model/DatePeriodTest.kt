package com.example.tracker.domain.model

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class DatePeriodTest {

    private val today = LocalDate.of(2026, 10, 3)

    @Test
    fun thisMonth_runsFromFirstOfMonthToToday() {
        assertEquals(LocalDate.of(2026, 10, 1), DatePeriod.ThisMonth.start(today))
        assertEquals(today, DatePeriod.ThisMonth.end(today))
    }

    @Test
    fun last30Days_includesTodayAnd29DaysBefore() {
        assertEquals(LocalDate.of(2026, 9, 4), DatePeriod.Last30Days.start(today))
        assertEquals(today, DatePeriod.Last30Days.end(today))
    }

    @Test
    fun custom_usesItsOwnDates() {
        val period = DatePeriod.Custom(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31))

        assertEquals(LocalDate.of(2026, 8, 1), period.start(today))
        assertEquals(LocalDate.of(2026, 8, 31), period.end(today))
    }

    @Test(expected = IllegalArgumentException::class)
    fun custom_endBeforeStart_isRejected() {
        DatePeriod.Custom(LocalDate.of(2026, 8, 31), LocalDate.of(2026, 8, 1))
    }
}
