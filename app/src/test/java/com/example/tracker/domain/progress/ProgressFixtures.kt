package com.example.tracker.domain.progress

import com.example.tracker.domain.model.ExerciseType
import com.example.tracker.domain.model.LoggedSet
import java.time.Instant

internal fun day(n: Int): Instant = Instant.parse("2026-08-01T10:00:00Z").plusSeconds(n * 86_400L)

internal fun weightSet(workout: String, finishedDay: Int, kg: Double, reps: Int, exercise: String = "bench") =
    LoggedSet(workout, day(finishedDay), exercise, ExerciseType.WEIGHT_REPS, kg, reps, null)

internal fun holdSet(workout: String, finishedDay: Int, seconds: Int, exercise: String = "plank") =
    LoggedSet(workout, day(finishedDay), exercise, ExerciseType.DURATION, null, null, seconds)

/** Bench Press sessions from the Exercise details artboard (top sets 40 → 50 kg). */
internal val benchHistory: List<LoggedSet> = listOf(
    Triple("w1", 0, listOf(35.0 to 10, 40.0 to 8, 40.0 to 7)),
    Triple("w2", 7, listOf(37.5 to 10, 40.0 to 8, 40.0 to 8)),
    Triple("w3", 14, listOf(40.0 to 10, 42.5 to 7, 42.5 to 6)),
    Triple("w4", 21, listOf(40.0 to 10, 42.5 to 8, 42.5 to 7)),
    Triple("w5", 28, listOf(40.0 to 10, 45.0 to 6, 45.0 to 6)),
    Triple("w6", 35, listOf(45.0 to 10, 50.0 to 8, 50.0 to 8)),
).flatMap { (workout, finished, sets) -> sets.map { (kg, reps) -> weightSet(workout, finished, kg, reps) } }
