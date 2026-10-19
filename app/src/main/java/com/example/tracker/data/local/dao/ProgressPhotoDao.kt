package com.example.tracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.example.tracker.data.local.entity.PhotoMeasurementEntity
import com.example.tracker.data.local.entity.PhotoSessionEntity
import com.example.tracker.data.local.entity.ProgressPhotoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressPhotoDao {

    /** Newest session first. */
    @Query("SELECT * FROM photo_session ORDER BY taken_at DESC")
    fun observeSessions(): Flow<List<PhotoSessionEntity>>

    /** Poses are ordered in Kotlin, since they are stored by name. */
    @Query("SELECT * FROM progress_photo")
    fun observePhotos(): Flow<List<ProgressPhotoEntity>>

    @Query("SELECT * FROM photo_measurement ORDER BY session_id, position")
    fun observeMeasurements(): Flow<List<PhotoMeasurementEntity>>

    @Query("SELECT * FROM photo_session WHERE id = :id")
    suspend fun getSession(id: String): PhotoSessionEntity?

    @Insert
    suspend fun insertSession(session: PhotoSessionEntity)

    /** Also deletes the session's photo rows and measurements. */
    @Query("DELETE FROM photo_session WHERE id = :id")
    suspend fun deleteSession(id: String)

    @Query("SELECT * FROM progress_photo WHERE session_id = :sessionId AND pose = :pose")
    suspend fun getPhoto(sessionId: String, pose: String): ProgressPhotoEntity?

    @Query("SELECT * FROM progress_photo WHERE session_id = :sessionId")
    suspend fun photosOf(sessionId: String): List<ProgressPhotoEntity>

    @Query("SELECT * FROM progress_photo WHERE id = :id")
    suspend fun getPhotoById(id: String): ProgressPhotoEntity?

    @Upsert
    suspend fun upsertPhoto(row: ProgressPhotoEntity)

    @Query("DELETE FROM progress_photo WHERE id = :id")
    suspend fun deletePhoto(id: String)

    @Query("DELETE FROM photo_measurement WHERE session_id = :sessionId")
    suspend fun deleteMeasurements(sessionId: String)

    @Insert
    suspend fun insertMeasurements(rows: List<PhotoMeasurementEntity>)

    /** Replaces the session's measurements with [rows]. */
    @Transaction
    suspend fun replaceMeasurements(sessionId: String, rows: List<PhotoMeasurementEntity>) {
        deleteMeasurements(sessionId)
        insertMeasurements(rows)
    }
}
