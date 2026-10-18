package com.example.tracker.domain.program

import com.example.tracker.domain.model.Program
import com.example.tracker.domain.model.WorkoutPlan
import com.example.tracker.domain.plan.suggestNextPlan
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

/**
 * Names the chosen training [days] after a split's day [labels], in weekday order and
 * wrapping around, e.g. Push, Pull, Legs on Mon–Sat gives Push, Pull, Legs, Push, Pull, Legs.
 * Without labels each day is named by [fallback], typically the weekday's name.
 */
fun nameTrainingDays(
    labels: List<String>,
    days: Set<DayOfWeek>,
    fallback: (DayOfWeek) -> String,
): Map<DayOfWeek, String> =
    days.sorted().withIndex().associate { (index, day) ->
        day to if (labels.isEmpty()) fallback(day) else labels[index % labels.size]
    }

/** A training day to do next; [date] is null for a day not tied to a weekday. */
data class NextWorkout(val day: WorkoutPlan, val date: LocalDate?)

/**
 * The day of [program] to train next as of [today]: the first scheduled day from today on,
 * skipping today's if it was already done today. A program without scheduled days falls back
 * to the unscheduled day done longest ago. Days without exercises can't be started, so they
 * are skipped. Null if nothing qualifies.
 */
fun nextWorkout(program: Program, today: LocalDate, zone: ZoneId): NextWorkout? {
    val startable = program.days.filter { it.exerciseCount > 0 }
    val scheduled = startable.mapNotNull { day ->
        val weekday = day.dayOfWeek ?: return@mapNotNull null
        var daysAhead = Math.floorMod(weekday.value - today.dayOfWeek.value, DAYS_PER_WEEK)
        val doneToday = day.lastDoneAt?.atZone(zone)?.toLocalDate() == today
        if (daysAhead == 0 && doneToday) daysAhead = DAYS_PER_WEEK
        day to today.plusDays(daysAhead.toLong())
    }
    scheduled.minByOrNull { (_, date) -> date }?.let { (day, date) -> return NextWorkout(day, date) }
    return suggestNextPlan(startable.filter { it.dayOfWeek == null })?.let { NextWorkout(it, date = null) }
}

private const val DAYS_PER_WEEK = 7
