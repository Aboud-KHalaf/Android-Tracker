package com.example.tracker.domain.repository

import com.example.tracker.domain.model.ExerciseSession
import com.example.tracker.domain.model.PersonalBest
import com.example.tracker.domain.model.SetType
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

    /** Completed sessions of one exercise, oldest first. Warm-up sets are left out. */
    fun observeExerciseSessions(exerciseId: String): Flow<List<ExerciseSession>>

    /** Completed sessions of every exercise, oldest first. */
    fun observeAllSessions(): Flow<List<ExerciseSession>>

    /** Every personal best ever set, oldest first. */
    fun observePersonalBests(): Flow<List<PersonalBest>>

    /** Completed working sets (not warm-ups) from the last finished workout that included the exercise. */
    suspend fun lastTimeSets(exerciseId: String): List<WorkoutSet>

    /** The note left on the exercise in the last finished workout that included it, if any. */
    suspend fun lastTimeNote(exerciseId: String): String?

    /**
     * Starts a workout from a plan and returns its id. Each exercise gets the plan's target
     * number of sets, prefilled from its targets and, where none is set, from last time.
     */
    suspend fun startWorkout(planId: String): String
    suspend fun finishWorkout(workoutId: String)

    /**
     * Sets the workout's note; blank text removes it. Throws [IllegalArgumentException] past
     * [com.example.tracker.domain.model.WorkoutNote.MAX_LENGTH].
     */
    suspend fun setWorkoutNote(workoutId: String, note: String?)

    /** Sets the note on one exercise of a workout; blank text removes it. */
    suspend fun setExerciseNote(workoutExerciseId: String, note: String?)

    /** Remembers which exercise is open, so an interrupted session reopens on it. */
    suspend fun setCurrentExercise(workoutId: String, exerciseIndex: Int)

    /** Deletes an in-progress or finished workout with all its sets. */
    suspend fun discardWorkout(workoutId: String)

    /**
     * Adds a set and returns its id. A working set goes after the last set and copies its
     * weight and reps; a warm-up goes after the last warm-up (so before the working sets) and
     * copies that warm-up, if any.
     */
    suspend fun addSet(workoutExerciseId: String, type: SetType = SetType.NORMAL): String
    suspend fun setSetType(setId: String, type: SetType)

    /** Rates how hard the set felt, from [com.example.tracker.domain.model.Rpe.MIN] to its MAX; null clears it. */
    suspend fun setSetRpe(setId: String, rpe: Double?)
    suspend fun updateSet(setId: String, weightKg: Double?, reps: Int?, durationSeconds: Int?)
    suspend fun completeSet(setId: String)
    suspend fun reopenSet(setId: String)
    suspend fun deleteSet(setId: String)
}
