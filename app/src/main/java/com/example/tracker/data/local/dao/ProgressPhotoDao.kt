package com.example.tracker.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.tracker.data.local.entity.ProgressPhotoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressPhotoDao {

    /** Newest day first. Poses are ordered in Kotlin, since they are stored by name. */
    @Query("SELECT * FROM progress_photo ORDER BY epoch_day DESC")
    fun observeAll(): Flow<List<ProgressPhotoEntity>>

    @Query("SELECT * FROM progress_photo WHERE epoch_day = :epochDay AND pose = :pose")
    suspend fun get(epochDay: Long, pose: String): ProgressPhotoEntity?

    @Query("SELECT * FROM progress_photo WHERE id = :id")
    suspend fun getById(id: String): ProgressPhotoEntity?

    @Upsert
    suspend fun upsert(row: ProgressPhotoEntity)

    @Query("DELETE FROM progress_photo WHERE id = :id")
    suspend fun delete(id: String)
}
