package com.example.tracker.domain.progress

import com.example.tracker.domain.model.Exercise
import com.example.tracker.domain.model.ExerciseSession
import com.example.tracker.domain.model.MuscleGroup
import java.time.Instant

/**
 * Working sets per muscle group in sessions finished from [from] (inclusive) to [to]
 * (exclusive). Sessions hold no warm-ups, so every set counts. Exercises without a group are
 * counted under null, so the user can see what isn't assigned yet.
 */
fun setsPerMuscleGroup(
    sessions: List<ExerciseSession>,
    exercises: List<Exercise>,
    from: Instant,
    to: Instant,
): Map<MuscleGroup?, Int> {
    val groups = exercises.associate { it.id to it.muscleGroup }
    return sessions
        .filter { it.finishedAt >= from && it.finishedAt < to }
        .groupingBy { groups[it.exerciseId] }
        .fold(0) { total, session -> total + session.sets.size }
}
