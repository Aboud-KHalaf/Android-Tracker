package com.example.tracker.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.tracker.data.local.entity.ExerciseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {

    @Query("SELECT * FROM exercises WHERE deleted_at IS NULL ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE id = :id AND deleted_at IS NULL")
    fun observe(id: String): Flow<ExerciseEntity?>

    @Query("SELECT * FROM exercises WHERE id = :id AND deleted_at IS NULL")
    suspend fun get(id: String): ExerciseEntity?

    /** Used for local writes and, later, for rows pulled from the backend. */
    @Upsert
    suspend fun upsert(rows: List<ExerciseEntity>)

    // Sync hooks

    /** Rows changed locally since the last push, tombstones included. */
    @Query("SELECT * FROM exercises WHERE sync_state = 'PENDING'")
    suspend fun pending(): List<ExerciseEntity>

    /** Marks pushed rows as synced, unless they changed again after [pushedUpTo]. */
    @Query("UPDATE exercises SET sync_state = 'SYNCED' WHERE id IN (:ids) AND updated_at <= :pushedUpTo")
    suspend fun markSynced(ids: List<String>, pushedUpTo: Long)
}
