package com.example.tracker.domain.nutrition

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class NutritionPeriodTest {

    private val today = LocalDate.of(2026, 10, 3)

    @Test
    fun thisMonth_runsFromFirstOfMonthToToday() {
        assertEquals(LocalDate.of(2026, 10, 1), NutritionPeriod.ThisMonth.start(today))
        assertEquals(today, NutritionPeriod.ThisMonth.end(today))
    }

    @Test
    fun last30Days_includesTodayAnd29DaysBefore() {
        assertEquals(LocalDate.of(2026, 9, 4), NutritionPeriod.Last30Days.start(today))
        assertEquals(today, NutritionPeriod.Last30Days.end(today))
    }

    @Test
    fun custom_usesItsOwnDates() {
        val period = NutritionPeriod.Custom(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31))

        assertEquals(LocalDate.of(2026, 8, 1), period.start(today))
        assertEquals(LocalDate.of(2026, 8, 31), period.end(today))
    }

    @Test(expected = IllegalArgumentException::class)
    fun custom_endBeforeStart_isRejected() {
        NutritionPeriod.Custom(LocalDate.of(2026, 8, 31), LocalDate.of(2026, 8, 1))
    }
}
