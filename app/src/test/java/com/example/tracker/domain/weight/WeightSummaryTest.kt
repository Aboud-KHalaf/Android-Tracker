package com.example.tracker.domain.weight

import com.example.tracker.domain.model.WeightEntry
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WeightSummaryTest {

    private val oct1 = LocalDate.of(2026, 10, 1)

    @Test
    fun of_noEntries_isNull() {
        assertNull(WeightSummary.of(emptyList()))
    }

    @Test
    fun of_comparesOldestToNewestWhateverTheOrder() {
        val entries = listOf(
            WeightEntry(oct1.plusDays(5), 79.0),
            WeightEntry(oct1, 80.5),
            WeightEntry(oct1.plusDays(9), 79.3),
        )

        val summary = WeightSummary.of(entries)!!

        assertEquals(oct1, summary.first.date)
        assertEquals(oct1.plusDays(9), summary.latest.date)
        assertEquals(-1.2, summary.changeKg, 1e-9)
        assertEquals(79.0, summary.lowestKg, 0.0)
        assertEquals(80.5, summary.highestKg, 0.0)
        assertEquals(3, summary.entryCount)
    }

    @Test
    fun of_singleEntry_hasNoChange() {
        assertEquals(0.0, WeightSummary.of(listOf(WeightEntry(oct1, 80.0)))!!.changeKg, 0.0)
    }
}
