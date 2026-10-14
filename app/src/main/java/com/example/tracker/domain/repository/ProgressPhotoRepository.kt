package com.example.tracker.domain.repository

import com.example.tracker.domain.model.PhotoPose
import com.example.tracker.domain.model.ProgressPhoto
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

/**
 * Progress photos, kept only on this device. The camera writes into a file from
 * [createCaptureFile]; [savePhoto] then keeps it, or [discardCapture] throws it away.
 * Storage failures propagate as exceptions.
 */
interface ProgressPhotoRepository {
    /** Every photo, newest day first, and within a day in [PhotoPose] order. */
    fun observePhotos(): Flow<List<ProgressPhoto>>

    /** A new, empty file for the camera to write one picture into. */
    suspend fun createCaptureFile(): File

    /** Keeps [capture] as the [pose] photo of [date], replacing one already taken that day. [capture] is moved. */
    suspend fun savePhoto(date: LocalDate, pose: PhotoPose, capture: File)

    /** Deletes a capture that wasn't saved. */
    suspend fun discardCapture(capture: File)

    /** Deletes the photo and its picture; this can't be undone. */
    suspend fun deletePhoto(id: String)
}
