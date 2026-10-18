package com.example.tracker.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.tracker.data.local.entity.ProgramEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgramDao {

    @Query("SELECT * FROM programs WHERE deleted_at IS NULL ORDER BY created_at")
    fun observeAll(): Flow<List<ProgramEntity>>

    @Query("SELECT * FROM programs WHERE deleted_at IS NULL ORDER BY created_at")
    suspend fun getAll(): List<ProgramEntity>

    @Query("SELECT * FROM programs WHERE id = :id AND deleted_at IS NULL")
    suspend fun get(id: String): ProgramEntity?

    @Upsert
    suspend fun upsert(rows: List<ProgramEntity>)

    /** Removes the exercises of every day of [programId]. */
    @Query(
        """
        UPDATE plan_exercises SET deleted_at = :now, updated_at = :now, sync_state = 'PENDING'
        WHERE deleted_at IS NULL AND plan_id IN (SELECT id FROM workout_plans WHERE program_id = :programId)
        """
    )
    suspend fun softDeleteExercisesOfProgram(programId: String, now: Long)

    @Query(
        """
        UPDATE workout_plans SET deleted_at = :now, updated_at = :now, sync_state = 'PENDING'
        WHERE program_id = :programId AND deleted_at IS NULL
        """
    )
    suspend fun softDeleteDaysOfProgram(programId: String, now: Long)

    // Sync hooks

    @Query("SELECT * FROM programs WHERE sync_state = 'PENDING'")
    suspend fun pending(): List<ProgramEntity>

    @Query("UPDATE programs SET sync_state = 'SYNCED' WHERE id IN (:ids) AND updated_at <= :pushedUpTo")
    suspend fun markSynced(ids: List<String>, pushedUpTo: Long)
}
