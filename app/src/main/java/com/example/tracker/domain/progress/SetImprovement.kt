package com.example.tracker.domain.progress

import com.example.tracker.domain.model.ExerciseType
import com.example.tracker.domain.model.SetImprovement
import com.example.tracker.domain.model.WorkoutSet

/**
 * How [current] improved on [previous] (the same set last time), or null if it didn't.
 * Weight takes precedence; reps count only when the weight is unchanged.
 */
fun improvementOver(type: ExerciseType, current: WorkoutSet, previous: WorkoutSet?): SetImprovement? {
    if (previous == null) return null
    return when (type) {
        ExerciseType.WEIGHT_REPS -> {
            val weight = current.weightKg ?: return null
            val previousWeight = previous.weightKg ?: return null
            val reps = current.reps ?: 0
            val previousReps = previous.reps ?: 0
            when {
                weight > previousWeight -> SetImprovement.Weight(weight - previousWeight)
                weight == previousWeight && reps > previousReps -> SetImprovement.Reps(reps - previousReps)
                else -> null
            }
        }

        ExerciseType.DURATION -> {
            val hold = current.durationSeconds ?: return null
            val previousHold = previous.durationSeconds ?: return null
            if (hold > previousHold) SetImprovement.Hold(hold - previousHold) else null
        }
    }
}
