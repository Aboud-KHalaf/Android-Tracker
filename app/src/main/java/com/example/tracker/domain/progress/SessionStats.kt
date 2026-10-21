package com.example.tracker.domain.progress

import com.example.tracker.domain.model.ExerciseSession
import com.example.tracker.domain.model.LoggedSet

/** Aggregates for one exercise in one workout, as plotted on the progress chart. */
data class SessionStats(
    val topWeightKg: Double?,
    /** Most reps done at [topWeightKg]. */
    val topSetReps: Int?,
    val totalReps: Int,
    /** Sum of weight × reps over all sets. */
    val volumeKg: Double,
    val longestHoldSeconds: Int?,
    /** Best one-rep max estimated from any set, kg; see [estimatedOneRepMaxKg]. */
    val estimatedOneRepMaxKg: Double?,
)

fun ExerciseSession.stats(): SessionStats {
    val topWeight = sets.mapNotNull { it.weightKg }.maxOrNull()
    return SessionStats(
        topWeightKg = topWeight,
        topSetReps = topWeight?.let { top ->
            sets.filter { it.weightKg == top }.mapNotNull { it.reps }.maxOrNull()
        },
        totalReps = sets.sumOf { it.reps ?: 0 },
        volumeKg = sets.sumOf { (it.weightKg ?: 0.0) * (it.reps ?: 0) },
        longestHoldSeconds = sets.mapNotNull { it.durationSeconds }.maxOrNull(),
        estimatedOneRepMaxKg = sets.mapNotNull { set ->
            val weight = set.weightKg ?: return@mapNotNull null
            val reps = set.reps ?: return@mapNotNull null
            estimatedOneRepMaxKg(weight, reps)
        }.maxOrNull(),
    )
}

/**
 * The most a lifter could likely lift once, from [weightKg] lifted [reps] times, by the Epley
 * formula. A single is taken as it is. Null without reps or weight, or past [MAX_ESTIMATE_REPS],
 * where the formula stops being a fair guess.
 */
fun estimatedOneRepMaxKg(weightKg: Double, reps: Int): Double? = when {
    weightKg <= 0.0 || reps < 1 || reps > MAX_ESTIMATE_REPS -> null
    reps == 1 -> weightKg
    else -> weightKg * (1 + reps / EPLEY_DIVISOR)
}

/** Above this many reps a set says more about endurance than about strength. */
const val MAX_ESTIMATE_REPS = 12

private const val EPLEY_DIVISOR = 30.0

/** Groups logged sets into one session per workout and exercise, oldest first. */
fun List<LoggedSet>.toSessions(): List<ExerciseSession> =
    groupBy { it.workoutId to it.exerciseId }
        .map { (key, sets) ->
            val first = sets.first()
            ExerciseSession(
                workoutId = key.first,
                exerciseId = key.second,
                exerciseType = first.exerciseType,
                finishedAt = first.finishedAt,
                sets = sets,
            )
        }
        .sortedBy { it.finishedAt }
