package com.example.tracker.domain.repository

import com.example.tracker.domain.model.ExerciseSession
import com.example.tracker.domain.model.PersonalBest
import com.example.tracker.domain.model.WeekSummary
import com.example.tracker.domain.model.Workout
import com.example.tracker.domain.model.WorkoutSet
import com.example.tracker.domain.model.WorkoutSummary
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

/**
 * Performed workouts, their sets, and the progress derived from them.
 * Write functions throw [IllegalArgumentException] for invalid input or unknown ids, and
 * [IllegalStateException] when the workout's state doesn't allow the change.
 */
interface WorkoutRepository {
    /** The workout in progress, or null if none. */
    fun observeActiveWorkout(): Flow<Workout?>
    fun observeWorkout(id: String): Flow<Workout?>

    /** Finished workouts, newest first, optionally only those started from [planId]. */
    fun observeHistory(planId: String? = null): Flow<List<WorkoutSummary>>
    fun observeWeekSummary(weekStart: LocalDate): Flow<WeekSummary>

    /** Completed sessions of one exercise, oldest first. */
    fun observeExerciseSessions(exerciseId: String): Flow<List<ExerciseSession>>

    /** Every personal best ever set, oldest first. */
    fun observePersonalBests(): Flow<List<PersonalBest>>

    /** Completed sets from the last finished workout that included the exercise. */
    suspend fun lastTimeSets(exerciseId: String): List<WorkoutSet>

    /**
     * Starts a workout from a plan and returns its id. Each exercise gets the plan's target
     * number of sets, prefilled from last time.
     */
    suspend fun startWorkout(planId: String): String
    suspend fun finishWorkout(workoutId: String)

    /** Deletes an in-progress or finished workout with all its sets. */
    suspend fun discardWorkout(workoutId: String)

    /** Adds a set after the last one, copying its weight and reps. Returns the set's id. */
    suspend fun addSet(workoutExerciseId: String): String
    suspend fun updateSet(setId: String, weightKg: Double?, reps: Int?, durationSeconds: Int?)
    suspend fun completeSet(setId: String)
    suspend fun reopenSet(setId: String)
    suspend fun deleteSet(setId: String)
}
