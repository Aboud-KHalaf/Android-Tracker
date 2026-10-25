package com.example.tracker.domain.repository

import com.example.tracker.domain.model.PhotoMeasurement
import com.example.tracker.domain.model.PhotoPose
import com.example.tracker.domain.photos.PhotoCheckIn
import java.io.File
import java.io.InputStream
import kotlinx.coroutines.flow.Flow

/**
 * Progress photos, kept only on this device and grouped into check-ins (photo sessions).
 * The camera writes into a file from [createCaptureFile], or [importCapture] copies a picture
 * from elsewhere, such as the gallery; [savePhoto] then keeps it, or [discardCapture] throws it
 * away. Storage failures propagate as exceptions.
 */
interface ProgressPhotoRepository {
    /** Every check-in, newest first; their photos are in [PhotoPose] order. */
    fun observeCheckIns(): Flow<List<PhotoCheckIn>>

    /** A new, empty file for the camera to write one picture into. */
    suspend fun createCaptureFile(): File

    /**
     * Copies the picture [open] reads into a new capture file and returns it. Throws
     * [IllegalArgumentException] when there is nothing to read; the partial copy is removed.
     */
    suspend fun importCapture(open: () -> InputStream): File

    /**
     * Keeps [capture] as the [pose] photo of the check-in [checkInId], replacing one already
     * taken in it; with a null id, starts a new check-in today. Returns the check-in's id.
     * [capture] is moved.
     */
    suspend fun savePhoto(checkInId: String?, pose: PhotoPose, capture: File): String

    /** Replaces the check-in's measurements. Throws [IllegalArgumentException] for invalid ones. */
    suspend fun saveMeasurements(checkInId: String, measurements: List<PhotoMeasurement>)

    /** Deletes a capture that wasn't saved. */
    suspend fun discardCapture(capture: File)

    /** Deletes the photo and its picture, and its check-in once that has no photos left; this can't be undone. */
    suspend fun deletePhoto(id: String)

    /** Deletes the check-in with its photos, pictures and measurements; this can't be undone. */
    suspend fun deleteCheckIn(id: String)
}
