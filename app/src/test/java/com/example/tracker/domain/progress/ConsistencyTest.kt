package com.example.tracker.domain.progress

import com.example.tracker.domain.model.WorkoutSummary
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class ConsistencyTest {

    // A Wednesday.
    private val today = LocalDate.of(2026, 10, 21)

    private fun workoutOn(date: LocalDate, id: String = date.toString()): WorkoutSummary {
        val start = date.atTime(18, 0).toInstant(ZoneOffset.UTC)
        return WorkoutSummary(id, "push", "Push", start, start.plusSeconds(3_000), 3, 0)
    }

    private fun streakOf(vararg dates: LocalDate) =
        consistencyOf(dates.map { workoutOn(it) }, today, ZoneOffset.UTC).weekStreak

    @Test
    fun weekStreak_countsWeeksInARowUpToThisOne() {
        assertEquals(3, streakOf(today, today.minusWeeks(1), today.minusWeeks(2), today.minusWeeks(4)))
    }

    @Test
    fun weekStreak_thisWeekNotTrainedYet_keepsLastWeeksStreak() {
        assertEquals(2, streakOf(today.minusWeeks(1), today.minusWeeks(2)))
    }

    @Test
    fun weekStreak_missedLastWeek_isZeroUntilTrained() {
        assertEquals(0, streakOf(today.minusWeeks(2)))
        assertEquals(1, streakOf(today, today.minusWeeks(2)))
    }

    @Test
    fun weekStreak_weeksStartOnMonday() {
        // Sunday the 18th is last week; Monday the 19th is this week.
        assertEquals(2, streakOf(LocalDate.of(2026, 10, 18), LocalDate.of(2026, 10, 19)))
    }

    @Test
    fun workoutsByDay_countsEachDay() {
        val consistency = consistencyOf(
            listOf(workoutOn(today, "a"), workoutOn(today, "b"), workoutOn(today.minusDays(1))),
            today,
            ZoneOffset.UTC,
        )

        assertEquals(mapOf(today to 2, today.minusDays(1) to 1), consistency.workoutsByDay)
    }
}
