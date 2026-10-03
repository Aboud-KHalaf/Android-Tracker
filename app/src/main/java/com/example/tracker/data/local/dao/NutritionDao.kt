package com.example.tracker.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.tracker.data.local.entity.DailyNutritionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NutritionDao {

    /** Logged days from [fromEpochDay] to [toEpochDay], both inclusive, newest first. */
    @Query(
        """
        SELECT * FROM daily_nutrition
        WHERE epoch_day BETWEEN :fromEpochDay AND :toEpochDay AND deleted_at IS NULL
        ORDER BY epoch_day DESC
        """
    )
    fun observeRange(fromEpochDay: Long, toEpochDay: Long): Flow<List<DailyNutritionEntity>>

    @Query("SELECT * FROM daily_nutrition WHERE epoch_day = :epochDay AND deleted_at IS NULL")
    fun observe(epochDay: Long): Flow<DailyNutritionEntity?>

    /** The row for [epochDay], including a deleted one, so it can be restored. */
    @Query("SELECT * FROM daily_nutrition WHERE epoch_day = :epochDay")
    suspend fun getIncludingDeleted(epochDay: Long): DailyNutritionEntity?

    @Upsert
    suspend fun upsert(rows: List<DailyNutritionEntity>)

    // Sync hooks

    @Query("SELECT * FROM daily_nutrition WHERE sync_state = 'PENDING'")
    suspend fun pending(): List<DailyNutritionEntity>
}
