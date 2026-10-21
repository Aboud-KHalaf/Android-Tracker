package com.example.tracker.domain.model

import java.time.DayOfWeek
import java.time.Instant

/**
 * A training day as shown in lists: one exercise list of a program, scheduled on [dayOfWeek]
 * or, when that is null, done whenever the user likes. [lastDoneAt] is when a workout from it
 * last finished.
 */
data class WorkoutPlan(
    val id: String,
    val name: String,
    val exerciseCount: Int,
    val lastDoneAt: Instant?,
    val programId: String? = null,
    val dayOfWeek: DayOfWeek? = null,
)

/** A training day with its exercises in order. */
data class PlanDetails(
    val id: String,
    val name: String,
    val exercises: List<PlanExercise>,
    val dayOfWeek: DayOfWeek? = null,
)

data class PlanExercise(
    val id: String,
    val exercise: Exercise,
    val position: Int,
    val config: ExerciseConfig,
    /** Shared by the exercises of a superset; see [com.example.tracker.domain.workout.Supersets]. */
    val supersetGroup: String? = null,
)

/**
 * An exercise's default configuration in a plan: what the user means to do, used to prefill
 * each new workout. What they actually did is recorded in the workout's sets.
 * Weight-and-reps exercises use [targetReps] and [targetWeightKg]; duration exercises use
 * [targetDurationSeconds]. A null target means "repeat last time".
 */
data class ExerciseConfig(
    val targetSets: Int = DEFAULT_TARGET_SETS,
    val targetReps: Int? = null,
    val targetWeightKg: Double? = null,
    val targetDurationSeconds: Int? = null,
) {
    /** Keeps only the targets that apply to [type], e.g. after replacing the exercise. */
    fun forType(type: ExerciseType): ExerciseConfig = when (type) {
        ExerciseType.WEIGHT_REPS -> copy(targetDurationSeconds = null)
        ExerciseType.DURATION -> copy(targetReps = null, targetWeightKg = null)
    }

    companion object {
        const val DEFAULT_TARGET_SETS = 3
        const val MAX_TARGET_SETS = 20
    }
}

/** A workout program, e.g. "Push/Pull/Legs", with its training days. */
data class Program(
    val id: String,
    val name: String,
    val isActive: Boolean,
    /** Scheduled days Monday to Sunday, then unscheduled ones. */
    val days: List<WorkoutPlan>,
) {
    val trainingDays: Set<DayOfWeek> get() = days.mapNotNullTo(mutableSetOf()) { it.dayOfWeek }
}
