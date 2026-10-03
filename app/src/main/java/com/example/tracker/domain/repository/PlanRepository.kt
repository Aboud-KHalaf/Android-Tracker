package com.example.tracker.domain.repository

import com.example.tracker.domain.model.PlanDetails
import com.example.tracker.domain.model.WorkoutPlan
import kotlinx.coroutines.flow.Flow

/**
 * Workout plans and their ordered exercises. Write functions throw
 * [IllegalArgumentException] for invalid input or unknown ids.
 */
interface PlanRepository {
    fun observePlans(): Flow<List<WorkoutPlan>>
    fun observePlan(id: String): Flow<PlanDetails?>

    /** Returns the new plan's id. */
    suspend fun createPlan(name: String): String
    suspend fun renamePlan(id: String, name: String)
    suspend fun deletePlan(id: String)

    /** Appends an exercise to the plan and returns the new plan exercise's id. */
    suspend fun addExercise(planId: String, exerciseId: String, targetSets: Int = DEFAULT_TARGET_SETS): String

    /** Removes an exercise from its plan; [restoreExercise] undoes this. */
    suspend fun removeExercise(planExerciseId: String)
    suspend fun restoreExercise(planExerciseId: String)

    /** Moves the exercise at [fromIndex] to [toIndex] in the plan's current order. */
    suspend fun moveExercise(planId: String, fromIndex: Int, toIndex: Int)

    companion object {
        const val DEFAULT_TARGET_SETS = 3
    }
}
