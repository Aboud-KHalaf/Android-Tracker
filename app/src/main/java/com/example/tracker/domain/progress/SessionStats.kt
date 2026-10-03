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
    )
}

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
