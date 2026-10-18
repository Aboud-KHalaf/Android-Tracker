package com.example.tracker.domain.program

import com.example.tracker.domain.model.Program
import com.example.tracker.domain.model.WorkoutPlan
import java.time.DayOfWeek
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProgramScheduleTest {

    // A Wednesday.
    private val today = LocalDate.of(2026, 10, 14)

    private fun day(id: String, weekday: DayOfWeek?, exercises: Int = 3, lastDoneAt: Instant? = null) =
        WorkoutPlan(id, id, exercises, lastDoneAt, programId = "p", dayOfWeek = weekday)

    private fun program(vararg days: WorkoutPlan) = Program("p", "Program", isActive = true, days = days.toList())

    @Test
    fun nameTrainingDays_wrapsSplitLabelsOverDaysInWeekOrder() {
        val names = nameTrainingDays(
            labels = listOf("Push", "Pull", "Legs"),
            days = setOf(SATURDAY, MONDAY, THURSDAY, TUESDAY),
            fallback = { it.name },
        )

        assertEquals(mapOf(MONDAY to "Push", TUESDAY to "Pull", THURSDAY to "Legs", SATURDAY to "Push"), names)
    }

    @Test
    fun nameTrainingDays_withoutLabels_usesTheFallback() {
        val names = nameTrainingDays(emptyList(), setOf(FRIDAY), fallback = { "Day ${it.value}" })

        assertEquals(mapOf(FRIDAY to "Day 5"), names)
    }

    @Test
    fun nextWorkout_isTodaysDayWhenScheduled() {
        val next = nextWorkout(program(day("mon", MONDAY), day("wed", WEDNESDAY)), today, ZoneOffset.UTC)

        assertEquals("wed", next?.day?.id)
        assertEquals(today, next?.date)
    }

    @Test
    fun nextWorkout_onARestDay_isTheNextScheduledDay_wrappingIntoNextWeek() {
        val next = nextWorkout(program(day("mon", MONDAY), day("tue", TUESDAY)), today, ZoneOffset.UTC)

        assertEquals("mon", next?.day?.id)
        assertEquals(LocalDate.of(2026, 10, 19), next?.date)
    }

    @Test
    fun nextWorkout_skipsTodaysDayOnceDone() {
        val doneThisMorning = today.atTime(8, 0).toInstant(ZoneOffset.UTC)
        val next = nextWorkout(
            program(day("wed", WEDNESDAY, lastDoneAt = doneThisMorning), day("fri", FRIDAY)),
            today,
            ZoneOffset.UTC,
        )

        assertEquals("fri", next?.day?.id)
    }

    @Test
    fun nextWorkout_skipsDaysWithoutExercises() {
        val next = nextWorkout(program(day("wed", WEDNESDAY, exercises = 0), day("fri", FRIDAY)), today, ZoneOffset.UTC)

        assertEquals("fri", next?.day?.id)
    }

    @Test
    fun nextWorkout_withoutSchedule_suggestsTheUnscheduledDayDoneLongestAgo() {
        val next = nextWorkout(
            program(
                day("a", null, lastDoneAt = Instant.parse("2026-10-12T10:00:00Z")),
                day("b", null, lastDoneAt = Instant.parse("2026-10-10T10:00:00Z")),
            ),
            today,
            ZoneOffset.UTC,
        )

        assertEquals("b", next?.day?.id)
        assertNull(next?.date)
    }

    @Test
    fun nextWorkout_isNullWhenNothingCanBeStarted() {
        assertNull(nextWorkout(program(day("wed", WEDNESDAY, exercises = 0)), today, ZoneOffset.UTC))
    }
}
