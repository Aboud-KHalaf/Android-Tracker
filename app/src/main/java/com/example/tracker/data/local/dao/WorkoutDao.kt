package com.example.tracker.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.tracker.data.local.entity.WorkoutEntity
import com.example.tracker.data.local.entity.WorkoutExerciseEntity
import com.example.tracker.data.local.entity.WorkoutSetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {

    // Workouts

    @Query(
        """
        SELECT * FROM workouts
        WHERE finished_at IS NULL AND deleted_at IS NULL
        ORDER BY started_at DESC LIMIT 1
        """
    )
    fun observeActive(): Flow<WorkoutEntity?>

    @Query(
        """
        SELECT * FROM workouts
        WHERE finished_at IS NULL AND deleted_at IS NULL
        ORDER BY started_at DESC LIMIT 1
        """
    )
    suspend fun getActive(): WorkoutEntity?

    @Query("SELECT * FROM workouts WHERE id = :id AND deleted_at IS NULL")
    fun observe(id: String): Flow<WorkoutEntity?>

    @Query("SELECT * FROM workouts WHERE id = :id AND deleted_at IS NULL")
    suspend fun get(id: String): WorkoutEntity?

    @Query(
        """
        SELECT w.id, w.plan_id, w.name, w.started_at, w.finished_at,
            (SELECT COUNT(*) FROM workout_exercises we
                WHERE we.workout_id = w.id AND we.deleted_at IS NULL) AS exercise_count
        FROM workouts w
        WHERE w.finished_at IS NOT NULL AND w.deleted_at IS NULL
            AND (:planId IS NULL OR w.plan_id = :planId)
        ORDER BY w.finished_at DESC
        """
    )
    fun observeFinishedSummaries(planId: String?): Flow<List<WorkoutSummaryRow>>

    // Exercises and sets of one workout

    @Query(
        """
        SELECT we.id, we.workout_id, we.position,
            e.id AS exercise_id, e.name AS exercise_name, e.type AS exercise_type
        FROM workout_exercises we
        JOIN exercises e ON e.id = we.exercise_id
        WHERE we.workout_id = :workoutId AND we.deleted_at IS NULL
        ORDER BY we.position
        """
    )
    fun observeExercises(workoutId: String): Flow<List<WorkoutExerciseRow>>

    @Query(
        """
        SELECT s.* FROM workout_sets s
        JOIN workout_exercises we ON we.id = s.workout_exercise_id
        WHERE we.workout_id = :workoutId AND we.deleted_at IS NULL AND s.deleted_at IS NULL
        ORDER BY s.position
        """
    )
    fun observeSets(workoutId: String): Flow<List<WorkoutSetEntity>>

    @Query("SELECT * FROM workout_exercises WHERE id = :id AND deleted_at IS NULL")
    suspend fun getWorkoutExercise(id: String): WorkoutExerciseEntity?

    @Query("SELECT * FROM workout_sets WHERE id = :id AND deleted_at IS NULL")
    suspend fun getSet(id: String): WorkoutSetEntity?

    @Query(
        """
        SELECT * FROM workout_sets
        WHERE workout_exercise_id = :workoutExerciseId AND deleted_at IS NULL
        ORDER BY position
        """
    )
    suspend fun getSets(workoutExerciseId: String): List<WorkoutSetEntity>

    @Query(
        """
        SELECT COALESCE(MAX(position), -1) FROM workout_sets
        WHERE workout_exercise_id = :workoutExerciseId AND deleted_at IS NULL
        """
    )
    suspend fun maxSetPosition(workoutExerciseId: String): Int

    // Progress

    /** Completed sets of the most recent finished workout that included [exerciseId]. */
    @Query(
        """
        SELECT s.* FROM workout_sets s
        WHERE s.deleted_at IS NULL AND s.completed_at IS NOT NULL
            AND s.workout_exercise_id = (
                SELECT we.id FROM workout_exercises we
                JOIN workouts w ON w.id = we.workout_id
                JOIN workout_sets cs ON cs.workout_exercise_id = we.id
                WHERE we.exercise_id = :exerciseId
                    AND we.deleted_at IS NULL AND w.deleted_at IS NULL
                    AND w.finished_at IS NOT NULL
                    AND cs.deleted_at IS NULL AND cs.completed_at IS NOT NULL
                ORDER BY w.finished_at DESC LIMIT 1
            )
        ORDER BY s.position
        """
    )
    suspend fun lastCompletedSets(exerciseId: String): List<WorkoutSetEntity>

    /** All completed sets in finished workouts, optionally for one exercise, oldest first. */
    @Query(
        """
        SELECT w.id AS workout_id, w.finished_at, e.id AS exercise_id, e.type AS exercise_type,
            s.id AS set_id, s.position, s.weight_kg, s.reps, s.duration_seconds, s.completed_at
        FROM workout_sets s
        JOIN workout_exercises we ON we.id = s.workout_exercise_id
        JOIN workouts w ON w.id = we.workout_id
        JOIN exercises e ON e.id = we.exercise_id
        WHERE s.deleted_at IS NULL AND s.completed_at IS NOT NULL
            AND we.deleted_at IS NULL AND w.deleted_at IS NULL AND w.finished_at IS NOT NULL
            AND (:exerciseId IS NULL OR e.id = :exerciseId)
        ORDER BY w.finished_at, we.position, s.position
        """
    )
    fun observeLoggedSets(exerciseId: String?): Flow<List<LoggedSetRow>>

    // Cascading soft deletes

    @Query(
        """
        UPDATE workout_sets SET deleted_at = :now, updated_at = :now, sync_state = 'PENDING'
        WHERE deleted_at IS NULL AND workout_exercise_id IN
            (SELECT id FROM workout_exercises WHERE workout_id = :workoutId)
        """
    )
    suspend fun softDeleteSetsOfWorkout(workoutId: String, now: Long)

    @Query(
        """
        UPDATE workout_exercises SET deleted_at = :now, updated_at = :now, sync_state = 'PENDING'
        WHERE workout_id = :workoutId AND deleted_at IS NULL
        """
    )
    suspend fun softDeleteExercisesOfWorkout(workoutId: String, now: Long)

    // Writes (local changes and, later, rows pulled from the backend)

    @Upsert
    suspend fun upsertWorkouts(rows: List<WorkoutEntity>)

    @Upsert
    suspend fun upsertWorkoutExercises(rows: List<WorkoutExerciseEntity>)

    @Upsert
    suspend fun upsertSets(rows: List<WorkoutSetEntity>)

    // Sync hooks

    @Query("SELECT * FROM workouts WHERE sync_state = 'PENDING'")
    suspend fun pendingWorkouts(): List<WorkoutEntity>

    @Query("SELECT * FROM workout_exercises WHERE sync_state = 'PENDING'")
    suspend fun pendingWorkoutExercises(): List<WorkoutExerciseEntity>

    @Query("SELECT * FROM workout_sets WHERE sync_state = 'PENDING'")
    suspend fun pendingSets(): List<WorkoutSetEntity>

    @Query("UPDATE workouts SET sync_state = 'SYNCED' WHERE id IN (:ids) AND updated_at <= :pushedUpTo")
    suspend fun markWorkoutsSynced(ids: List<String>, pushedUpTo: Long)

    @Query("UPDATE workout_exercises SET sync_state = 'SYNCED' WHERE id IN (:ids) AND updated_at <= :pushedUpTo")
    suspend fun markWorkoutExercisesSynced(ids: List<String>, pushedUpTo: Long)

    @Query("UPDATE workout_sets SET sync_state = 'SYNCED' WHERE id IN (:ids) AND updated_at <= :pushedUpTo")
    suspend fun markSetsSynced(ids: List<String>, pushedUpTo: Long)
}
