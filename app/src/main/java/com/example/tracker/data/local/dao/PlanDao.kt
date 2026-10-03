package com.example.tracker.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.tracker.data.local.entity.PlanEntity
import com.example.tracker.data.local.entity.PlanExerciseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanDao {

    @Query(
        """
        SELECT p.id, p.name,
            (SELECT COUNT(*) FROM plan_exercises pe
                JOIN exercises e ON e.id = pe.exercise_id
                WHERE pe.plan_id = p.id AND pe.deleted_at IS NULL AND e.deleted_at IS NULL
            ) AS exercise_count,
            (SELECT MAX(w.finished_at) FROM workouts w
                WHERE w.plan_id = p.id AND w.deleted_at IS NULL AND w.finished_at IS NOT NULL
            ) AS last_done_at
        FROM workout_plans p
        WHERE p.deleted_at IS NULL
        ORDER BY p.created_at
        """
    )
    fun observeSummaries(): Flow<List<PlanSummaryRow>>

    @Query("SELECT * FROM workout_plans WHERE id = :id AND deleted_at IS NULL")
    fun observe(id: String): Flow<PlanEntity?>

    @Query("SELECT * FROM workout_plans WHERE id = :id AND deleted_at IS NULL")
    suspend fun get(id: String): PlanEntity?

    @Query(
        """
        SELECT pe.id, pe.plan_id, pe.position, pe.target_sets,
            e.id AS exercise_id, e.name AS exercise_name, e.type AS exercise_type
        FROM plan_exercises pe
        JOIN exercises e ON e.id = pe.exercise_id
        WHERE pe.plan_id = :planId AND pe.deleted_at IS NULL AND e.deleted_at IS NULL
        ORDER BY pe.position
        """
    )
    fun observeExercises(planId: String): Flow<List<PlanExerciseRow>>

    @Query(
        """
        SELECT * FROM plan_exercises
        WHERE plan_id = :planId AND deleted_at IS NULL
        ORDER BY position
        """
    )
    suspend fun getPlanExercises(planId: String): List<PlanExerciseEntity>

    /** Includes soft-deleted rows, so a removal can be undone. */
    @Query("SELECT * FROM plan_exercises WHERE id = :id")
    suspend fun getPlanExerciseIncludingDeleted(id: String): PlanExerciseEntity?

    @Query("SELECT COALESCE(MAX(position), -1) FROM plan_exercises WHERE plan_id = :planId AND deleted_at IS NULL")
    suspend fun maxPosition(planId: String): Int

    @Query(
        """
        UPDATE plan_exercises SET deleted_at = :now, updated_at = :now, sync_state = 'PENDING'
        WHERE plan_id = :planId AND deleted_at IS NULL
        """
    )
    suspend fun softDeleteExercisesOfPlan(planId: String, now: Long)

    @Query(
        """
        UPDATE plan_exercises SET deleted_at = :now, updated_at = :now, sync_state = 'PENDING'
        WHERE exercise_id = :exerciseId AND deleted_at IS NULL
        """
    )
    suspend fun softDeleteByExercise(exerciseId: String, now: Long)

    @Upsert
    suspend fun upsertPlans(rows: List<PlanEntity>)

    @Upsert
    suspend fun upsertPlanExercises(rows: List<PlanExerciseEntity>)

    // Sync hooks

    @Query("SELECT * FROM workout_plans WHERE sync_state = 'PENDING'")
    suspend fun pendingPlans(): List<PlanEntity>

    @Query("SELECT * FROM plan_exercises WHERE sync_state = 'PENDING'")
    suspend fun pendingPlanExercises(): List<PlanExerciseEntity>

    @Query("UPDATE workout_plans SET sync_state = 'SYNCED' WHERE id IN (:ids) AND updated_at <= :pushedUpTo")
    suspend fun markPlansSynced(ids: List<String>, pushedUpTo: Long)

    @Query("UPDATE plan_exercises SET sync_state = 'SYNCED' WHERE id IN (:ids) AND updated_at <= :pushedUpTo")
    suspend fun markPlanExercisesSynced(ids: List<String>, pushedUpTo: Long)
}
