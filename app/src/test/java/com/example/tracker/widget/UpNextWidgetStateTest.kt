package com.example.tracker.widget

import com.example.tracker.domain.model.Program
import com.example.tracker.domain.model.Workout
import com.example.tracker.domain.model.WorkoutPlan
import com.example.tracker.domain.model.WorkoutSummary
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class UpNextWidgetStateTest {

    // Monday 28 September 2026.
    private val today = LocalDate.of(2026, 9, 28)
    private val zone = ZoneOffset.UTC

    private fun day(id: String, weekday: DayOfWeek?, exercises: Int = 4) =
        WorkoutPlan(id, id, exercises, null, programId = "p", dayOfWeek = weekday)

    private fun finishedOn(date: LocalDate): WorkoutSummary {
        val start = date.atTime(18, 0).toInstant(zone)
        return WorkoutSummary(date.toString(), "push", "Push", start, start.plusSeconds(3_000), 4, 0)
    }

    @Test
    fun workoutInProgress_isResumed() {
        val active = Workout("w1", "push", "Push Day", Instant.parse("2026-09-28T08:00:00Z"), null, emptyList())

        val state = UpNextWidgetState.of(today, zone, active, emptyList(), emptyList())

        assertEquals(UpNextWidgetState.UpNext.Resume("Push Day"), state.upNext)
    }

    @Test
    fun nextScheduledDay_isOfferedWithHowFarOffItIs() {
        val program = Program("p", "PPL", isActive = true, days = listOf(day("Pull", DayOfWeek.WEDNESDAY)))

        val start = UpNextWidgetState.of(today, zone, null, listOf(program), emptyList()).upNext as UpNextWidgetState.UpNext.Start

        assertEquals("Pull", start.name)
        assertEquals(4, start.exerciseCount)
        assertEquals(2L, start.daysAhead(today))
    }

    @Test
    fun noProgramOrNoExercises_hasNothingToStart() {
        val empty = Program("p", "PPL", isActive = true, days = listOf(day("Push", DayOfWeek.MONDAY, exercises = 0)))

        assertEquals(UpNextWidgetState.UpNext.NothingToStart, UpNextWidgetState.of(today, zone, null, emptyList(), emptyList()).upNext)
        assertEquals(UpNextWidgetState.UpNext.NothingToStart, UpNextWidgetState.of(today, zone, null, listOf(empty), emptyList()).upNext)
    }

    @Test
    fun workoutsThisWeek_countsFromMonday() {
        val history = listOf(finishedOn(today), finishedOn(today.minusDays(1)))

        assertEquals(1, UpNextWidgetState.of(today, zone, null, emptyList(), history).workoutsThisWeek)
    }
}
