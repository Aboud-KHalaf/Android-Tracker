package com.example.tracker.domain.progress

import com.example.tracker.domain.model.ExerciseType
import com.example.tracker.domain.model.SetImprovement
import com.example.tracker.domain.model.WorkoutSet

/**
 * How [current] improved on [previous] (the same set last time), or null if it didn't.
 * Weight takes precedence; reps count only when the weight is unchanged.
 */
fun improvementOver(type: ExerciseType, current: WorkoutSet, previous: WorkoutSet?): SetImprovement? =
    previous?.let {
        when (type) {
            ExerciseType.WEIGHT_REPS -> weightRepsImprovement(current, it)
            ExerciseType.DURATION -> holdImprovement(current, it)
        }
    }

/** Heavier wins; at the same weight, more reps. */
private fun weightRepsImprovement(current: WorkoutSet, previous: WorkoutSet): SetImprovement? {
    val weight = current.weightKg ?: return null
    val previousWeight = previous.weightKg ?: return null
    val reps = current.reps ?: 0
    val previousReps = previous.reps ?: 0
    return when {
        weight > previousWeight -> SetImprovement.Weight(weight - previousWeight)
        weight == previousWeight && reps > previousReps -> SetImprovement.Reps(reps - previousReps)
        else -> null
    }
}

private fun holdImprovement(current: WorkoutSet, previous: WorkoutSet): SetImprovement? {
    val hold = current.durationSeconds ?: return null
    val previousHold = previous.durationSeconds ?: return null
    return if (hold > previousHold) SetImprovement.Hold(hold - previousHold) else null
}
