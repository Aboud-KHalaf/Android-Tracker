package com.example.tracker.domain.model

import java.time.Instant

/** A plan as shown in plan lists. [lastDoneAt] is when a workout from it last finished. */
data class WorkoutPlan(
    val id: String,
    val name: String,
    val exerciseCount: Int,
    val lastDoneAt: Instant?,
)

/** A plan with its exercises in order. */
data class PlanDetails(
    val id: String,
    val name: String,
    val exercises: List<PlanExercise>,
)

data class PlanExercise(
    val id: String,
    val exercise: Exercise,
    val position: Int,
    val targetSets: Int,
)
