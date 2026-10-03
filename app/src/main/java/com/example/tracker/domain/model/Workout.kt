package com.example.tracker.domain.model

import java.time.Duration
import java.time.Instant

/** A performed workout with its exercises and sets. [finishedAt] is null while in progress. */
data class Workout(
    val id: String,
    val planId: String?,
    val name: String,
    val startedAt: Instant,
    val finishedAt: Instant?,
    val exercises: List<WorkoutExercise>,
) {
    val isInProgress: Boolean get() = finishedAt == null
}

data class WorkoutExercise(
    val id: String,
    val exercise: Exercise,
    val position: Int,
    val sets: List<WorkoutSet>,
)

/**
 * One set. Weight-and-reps exercises use [weightKg] and [reps]; duration exercises use
 * [durationSeconds]. [completedAt] is null until the set is completed.
 */
data class WorkoutSet(
    val id: String,
    val position: Int,
    val weightKg: Double?,
    val reps: Int?,
    val durationSeconds: Int?,
    val completedAt: Instant?,
) {
    val isCompleted: Boolean get() = completedAt != null
}

/** A finished workout as shown in history lists. */
data class WorkoutSummary(
    val id: String,
    val planId: String?,
    val name: String,
    val startedAt: Instant,
    val finishedAt: Instant,
    val exerciseCount: Int,
    val personalBestCount: Int,
) {
    val duration: Duration get() = Duration.between(startedAt, finishedAt)
}
