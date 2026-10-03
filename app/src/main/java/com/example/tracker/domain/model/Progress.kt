package com.example.tracker.domain.model

import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant

/** A completed set in a finished workout. */
data class LoggedSet(
    val workoutId: String,
    val finishedAt: Instant,
    val exerciseId: String,
    val exerciseType: ExerciseType,
    val weightKg: Double?,
    val reps: Int?,
    val durationSeconds: Int?,
)

/** The completed sets of one exercise in one finished workout. */
data class ExerciseSession(
    val workoutId: String,
    val exerciseId: String,
    val exerciseType: ExerciseType,
    val finishedAt: Instant,
    val sets: List<LoggedSet>,
)

/**
 * A personal best: the top set (weight × reps) or longest hold (duration) of the
 * session that achieved it.
 */
data class PersonalBest(
    val workoutId: String,
    val exerciseId: String,
    val exerciseType: ExerciseType,
    val achievedAt: Instant,
    val weightKg: Double?,
    val reps: Int?,
    val durationSeconds: Int?,
)

/** Training in one calendar week (Monday to Sunday). */
data class WeekSummary(
    val workoutCount: Int,
    val totalDuration: Duration,
    val trainedDays: Set<DayOfWeek>,
)

/** How a set improved on the same set last time; drives the "+2.5 kg" style badges. */
sealed interface SetImprovement {
    data class Weight(val kg: Double) : SetImprovement
    data class Reps(val reps: Int) : SetImprovement
    data class Hold(val seconds: Int) : SetImprovement
}
