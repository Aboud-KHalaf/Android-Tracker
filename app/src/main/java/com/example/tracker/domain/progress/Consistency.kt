package com.example.tracker.domain.progress

import com.example.tracker.domain.model.WorkoutSummary
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/** How regularly the user trains: a streak of weeks and the workouts on each recent day. */
data class Consistency(
    /** Weeks in a row, up to this one, with at least one workout; this week counts once it has one. */
    val weekStreak: Int,
    /** Finished workouts per day, for days with any, over all history. */
    val workoutsByDay: Map<LocalDate, Int>,
)

/** Works out [Consistency] from finished [workouts], as of [today] in [zone]. Weeks start on Monday. */
fun consistencyOf(workouts: List<WorkoutSummary>, today: LocalDate, zone: ZoneId): Consistency {
    val byDay = workouts.groupingBy { it.startedAt.atZone(zone).toLocalDate() }.eachCount()
    val trainedWeeks = byDay.keys.map { it.startOfWeek() }.toSet()
    // A week still under way doesn't break the streak; it only adds to it once trained.
    var week = today.startOfWeek()
    if (week !in trainedWeeks) week = week.minusWeeks(1)
    var streak = 0
    while (week in trainedWeeks) {
        streak++
        week = week.minusWeeks(1)
    }
    return Consistency(weekStreak = streak, workoutsByDay = byDay)
}

fun LocalDate.startOfWeek(): LocalDate = with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
