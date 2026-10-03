package com.example.tracker.domain.progress

import com.example.tracker.domain.model.WeekSummary
import com.example.tracker.domain.model.WorkoutSummary
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId

/** Summarizes the finished workouts that started in the 7 days from [weekStart] in [zone]. */
fun summarizeWeek(workouts: List<WorkoutSummary>, weekStart: LocalDate, zone: ZoneId): WeekSummary {
    val from = weekStart.atStartOfDay(zone).toInstant()
    val to = weekStart.plusDays(7).atStartOfDay(zone).toInstant()
    val inWeek = workouts.filter { it.startedAt >= from && it.startedAt < to }
    return WeekSummary(
        workoutCount = inWeek.size,
        totalDuration = inWeek.fold(Duration.ZERO) { total, w -> total + w.duration },
        trainedDays = inWeek.map { it.startedAt.atZone(zone).dayOfWeek }.toSet(),
    )
}
