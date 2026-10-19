package com.example.tracker.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.tracker.data.local.entity.CustomStatEntity
import com.example.tracker.data.local.entity.CustomStatEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomStatDao {

    @Query("SELECT * FROM custom_stats WHERE deleted_at IS NULL ORDER BY created_at, id")
    fun observeStats(): Flow<List<CustomStatEntity>>

    @Query("SELECT * FROM custom_stats WHERE id = :id")
    suspend fun getStatIncludingDeleted(id: String): CustomStatEntity?

    @Upsert
    suspend fun upsertStat(stat: CustomStatEntity)

    /** Each stat's first and latest live entry (one row when they are the same). */
    @Query(
        """
        SELECT * FROM custom_stat_entries AS e
        WHERE e.deleted_at IS NULL AND (
            e.epoch_day = (SELECT MIN(epoch_day) FROM custom_stat_entries WHERE stat_id = e.stat_id AND deleted_at IS NULL)
            OR e.epoch_day = (SELECT MAX(epoch_day) FROM custom_stat_entries WHERE stat_id = e.stat_id AND deleted_at IS NULL)
        )
        """
    )
    fun observeFirstAndLatestEntries(): Flow<List<CustomStatEntryEntity>>

    /** Entries of [statId] from [fromEpochDay] to [toEpochDay], both inclusive, newest first. */
    @Query(
        """
        SELECT * FROM custom_stat_entries
        WHERE stat_id = :statId AND epoch_day BETWEEN :fromEpochDay AND :toEpochDay AND deleted_at IS NULL
        ORDER BY epoch_day DESC
        """
    )
    fun observeEntries(statId: String, fromEpochDay: Long, toEpochDay: Long): Flow<List<CustomStatEntryEntity>>

    @Query("SELECT * FROM custom_stat_entries WHERE stat_id = :statId AND epoch_day = :epochDay AND deleted_at IS NULL")
    fun observeEntry(statId: String, epochDay: Long): Flow<CustomStatEntryEntity?>

    /** The row for that stat and day, including a deleted one, so it can be restored. */
    @Query("SELECT * FROM custom_stat_entries WHERE stat_id = :statId AND epoch_day = :epochDay")
    suspend fun getEntryIncludingDeleted(statId: String, epochDay: Long): CustomStatEntryEntity?

    @Upsert
    suspend fun upsertEntry(entry: CustomStatEntryEntity)

    // Sync hooks

    @Query("SELECT * FROM custom_stats WHERE sync_state = 'PENDING'")
    suspend fun pendingStats(): List<CustomStatEntity>

    @Query("SELECT * FROM custom_stat_entries WHERE sync_state = 'PENDING'")
    suspend fun pendingEntries(): List<CustomStatEntryEntity>
}
