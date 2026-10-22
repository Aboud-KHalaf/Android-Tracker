package com.example.tracker.domain.repository

import com.example.tracker.domain.model.ExerciseConfig
import com.example.tracker.domain.model.PlanDetails
import com.example.tracker.domain.model.WorkoutPlan
import kotlinx.coroutines.flow.Flow

/**
 * Training days ("plans") and their ordered exercises. Days are created through
 * [ProgramRepository]. Write functions throw [IllegalArgumentException] for invalid input or
 * unknown ids.
 */
interface PlanRepository {
    fun observePlans(): Flow<List<WorkoutPlan>>
    fun observePlan(id: String): Flow<PlanDetails?>

    suspend fun renamePlan(id: String, name: String)
    suspend fun deletePlan(id: String)

    /** Appends an exercise to the plan and returns the new plan exercise's id. */
    suspend fun addExercise(planId: String, exerciseId: String, config: ExerciseConfig = ExerciseConfig()): String

    /**
     * Swaps the library exercise behind a plan exercise, keeping its place and the targets
     * that still apply to the new exercise's type.
     */
    suspend fun replaceExercise(planExerciseId: String, exerciseId: String)

    /** Saves one exercise's default configuration; the plan's other exercises are untouched. */
    suspend fun updateExerciseConfig(planExerciseId: String, config: ExerciseConfig)

    /** Removes an exercise from its plan; [restoreExercise] undoes this. */
    suspend fun removeExercise(planExerciseId: String)
    suspend fun restoreExercise(planExerciseId: String)

    /**
     * Makes the exercise and the one after it a superset, joining any superset either is
     * already in. Does nothing for the last exercise.
     */
    suspend fun supersetWithNext(planExerciseId: String)

    /** Takes the exercise out of its superset; a superset left with one exercise ends. */
    suspend fun removeFromSuperset(planExerciseId: String)

    /** Moves the exercise at [fromIndex] to [toIndex] in the plan's current order. */
    suspend fun moveExercise(planId: String, fromIndex: Int, toIndex: Int)
}
