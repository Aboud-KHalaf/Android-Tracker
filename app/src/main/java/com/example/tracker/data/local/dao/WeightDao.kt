package com.example.tracker.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.tracker.data.local.entity.BodyWeightEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightDao {

    /** Entries from [fromEpochDay] to [toEpochDay], both inclusive, newest first. */
    @Query(
        """
        SELECT * FROM body_weight
        WHERE epoch_day BETWEEN :fromEpochDay AND :toEpochDay AND deleted_at IS NULL
        ORDER BY epoch_day DESC
        """
    )
    fun observeRange(fromEpochDay: Long, toEpochDay: Long): Flow<List<BodyWeightEntity>>

    @Query("SELECT * FROM body_weight WHERE deleted_at IS NULL ORDER BY epoch_day DESC LIMIT 1")
    fun observeLatest(): Flow<BodyWeightEntity?>

    @Query("SELECT * FROM body_weight WHERE epoch_day = :epochDay AND deleted_at IS NULL")
    fun observe(epochDay: Long): Flow<BodyWeightEntity?>

    /** The row for [epochDay], including a deleted one, so it can be restored. */
    @Query("SELECT * FROM body_weight WHERE epoch_day = :epochDay")
    suspend fun getIncludingDeleted(epochDay: Long): BodyWeightEntity?

    @Upsert
    suspend fun upsert(rows: List<BodyWeightEntity>)

    // Sync hooks

    @Query("SELECT * FROM body_weight WHERE sync_state = 'PENDING'")
    suspend fun pending(): List<BodyWeightEntity>
}
