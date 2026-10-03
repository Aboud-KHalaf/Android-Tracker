package com.example.tracker.domain.progress

import com.example.tracker.domain.model.ExerciseType
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressMetricTest {

    private val session = listOf(
        weightSet("w1", 0, 40.0, 10),
        weightSet("w1", 0, 45.0, 8),
        weightSet("w1", 0, 45.0, 6),
    ).toSessions().single()

    @Test
    fun valueOf_weightRepsAndVolume() {
        assertEquals(45.0, ProgressMetric.WEIGHT.valueOf(session)!!, 0.0)
        assertEquals(24.0, ProgressMetric.REPS.valueOf(session)!!, 0.0)
        assertEquals(1_030.0, ProgressMetric.VOLUME.valueOf(session)!!, 0.0)
        assertNull(ProgressMetric.HOLD.valueOf(session))
    }

    @Test
    fun valueOf_hold() {
        val hold = listOf(holdSet("p1", 0, 45), holdSet("p1", 0, 50)).toSessions().single()

        assertEquals(50.0, ProgressMetric.HOLD.valueOf(hold)!!, 0.0)
        assertNull(ProgressMetric.WEIGHT.valueOf(hold))
        assertNull(ProgressMetric.REPS.valueOf(hold))
    }

    @Test
    fun forType_listsDefaultFirst() {
        assertEquals(
            listOf(ProgressMetric.WEIGHT, ProgressMetric.REPS, ProgressMetric.VOLUME),
            ProgressMetric.forType(ExerciseType.WEIGHT_REPS),
        )
        assertEquals(listOf(ProgressMetric.HOLD), ProgressMetric.forType(ExerciseType.DURATION))
    }

    @Test
    fun timeRange_countsBackCalendarMonths() {
        val now = Instant.parse("2026-10-03T09:00:00Z")
        val zone = ZoneOffset.UTC

        assertTrue(TimeRange.ONE_MONTH.contains(Instant.parse("2026-09-03T09:00:00Z"), now, zone))
        assertFalse(TimeRange.ONE_MONTH.contains(Instant.parse("2026-09-03T08:59:59Z"), now, zone))
        assertTrue(TimeRange.THREE_MONTHS.contains(Instant.parse("2026-07-10T00:00:00Z"), now, zone))
        assertFalse(TimeRange.ONE_YEAR.contains(Instant.parse("2025-10-01T00:00:00Z"), now, zone))
        assertTrue(TimeRange.ALL.contains(Instant.parse("2001-01-01T00:00:00Z"), now, zone))
    }
}
