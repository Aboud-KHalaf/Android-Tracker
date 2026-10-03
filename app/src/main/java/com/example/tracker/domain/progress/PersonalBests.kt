package com.example.tracker.domain.progress

import com.example.tracker.domain.model.ExerciseSession
import com.example.tracker.domain.model.ExerciseType
import com.example.tracker.domain.model.PersonalBest

/**
 * Sessions that beat every earlier session of the same exercise, oldest first.
 * Compares top-set weight for weight exercises and longest hold for duration exercises.
 * An exercise's first session only sets the baseline, so it is never a personal best.
 */
fun findPersonalBests(sessions: List<ExerciseSession>): List<PersonalBest> =
    sessions.groupBy { it.exerciseId }
        .values
        .flatMap { exerciseSessions ->
            var best: Double? = null
            exerciseSessions.sortedBy { it.finishedAt }.mapNotNull { session ->
                val value = session.bestValue() ?: return@mapNotNull null
                val previous = best
                if (previous == null || value > previous) best = value
                if (previous != null && value > previous) session.toPersonalBest() else null
            }
        }
        .sortedBy { it.achievedAt }

/** The best session so far for one exercise (the earliest one, on ties), or null if none. */
fun currentPersonalBest(sessions: List<ExerciseSession>): PersonalBest? =
    sessions.filter { it.bestValue() != null }
        .maxWithOrNull(compareBy<ExerciseSession> { it.bestValue() }.thenByDescending { it.finishedAt })
        ?.toPersonalBest()

private fun ExerciseSession.bestValue(): Double? {
    val stats = stats()
    return when (exerciseType) {
        ExerciseType.WEIGHT_REPS -> stats.topWeightKg
        ExerciseType.DURATION -> stats.longestHoldSeconds?.toDouble()
    }
}

private fun ExerciseSession.toPersonalBest(): PersonalBest {
    val stats = stats()
    val isWeight = exerciseType == ExerciseType.WEIGHT_REPS
    return PersonalBest(
        workoutId = workoutId,
        exerciseId = exerciseId,
        exerciseType = exerciseType,
        achievedAt = finishedAt,
        weightKg = if (isWeight) stats.topWeightKg else null,
        reps = if (isWeight) stats.topSetReps else null,
        durationSeconds = if (isWeight) null else stats.longestHoldSeconds,
    )
}
