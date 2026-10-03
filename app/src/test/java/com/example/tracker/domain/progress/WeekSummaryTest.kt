package com.example.tracker.domain.progress

import com.example.tracker.domain.model.WorkoutSummary
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class WeekSummaryTest {

    private val zone = ZoneId.of("Europe/Berlin")
    private val monday = LocalDate.of(2026, 9, 28)

    private fun workout(start: LocalDateTime, minutes: Long): WorkoutSummary {
        val started = start.atZone(zone).toInstant()
        return WorkoutSummary("id-$start", null, "Push Day", started, started.plus(Duration.ofMinutes(minutes)), 5, 0)
    }

    @Test
    fun summarizeWeek_countsOnlyWorkoutsStartedInThatWeek() {
        val workouts = listOf(
            workout(monday.atTime(18, 0), 52),
            workout(monday.plusDays(2).atTime(7, 30), 48),
            workout(monday.plusDays(4).atTime(19, 0), 55),
            workout(monday.minusDays(1).atTime(23, 59), 60), // previous Sunday
            workout(monday.plusDays(7).atTime(0, 0), 30), // next Monday
        )

        val summary = summarizeWeek(workouts, monday, zone)

        assertEquals(3, summary.workoutCount)
        assertEquals(Duration.ofMinutes(155), summary.totalDuration)
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY), summary.trainedDays)
    }

    @Test
    fun summarizeWeek_noWorkouts_isEmpty() {
        val summary = summarizeWeek(emptyList(), monday, zone)

        assertEquals(0, summary.workoutCount)
        assertEquals(Duration.ZERO, summary.totalDuration)
        assertEquals(emptySet<DayOfWeek>(), summary.trainedDays)
    }
}
