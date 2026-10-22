package com.example.tracker.domain.progress

import com.example.tracker.domain.model.ExerciseSession
import com.example.tracker.domain.model.ExerciseType
import com.example.tracker.domain.model.WorkoutSet

/**
 * The completed sets of one exercise in a workout that set a new personal best: heavier than
 * [previousBestValue] (top weight, or longest hold for timed exercises) and than every set
 * completed before it in this workout. Warm-ups are never records. Like [findPersonalBests], an exercise's first workout
 * only sets the baseline, so without a previous best nothing is a record.
 */
fun recordSetIds(type: ExerciseType, sets: List<WorkoutSet>, previousBestValue: Double?): Set<String> {
    var best = previousBestValue ?: return emptySet()
    val records = mutableSetOf<String>()
    sets.filter { it.isCompleted && !it.type.isWarmUp }.sortedBy { it.completedAt }.forEach { set ->
        val value = when (type) {
            ExerciseType.WEIGHT_REPS -> set.weightKg
            ExerciseType.DURATION -> set.durationSeconds?.toDouble()
        } ?: return@forEach
        if (value > best) {
            best = value
            records += set.id
        }
    }
    return records
}

/** The value [recordSetIds] compares with: the all-time best's top weight or longest hold. */
fun bestValue(type: ExerciseType, sessions: List<ExerciseSession>): Double? =
    currentPersonalBest(sessions)?.let { best ->
        when (type) {
            ExerciseType.WEIGHT_REPS -> best.weightKg
            ExerciseType.DURATION -> best.durationSeconds?.toDouble()
        }
    }
