package com.example.tracker.data.repository

import com.example.tracker.core.IdGenerator
import com.example.tracker.core.TimeProvider
import com.example.tracker.data.local.ProgressPhotoStore
import com.example.tracker.data.local.TrackerDatabase
import com.example.tracker.data.local.entity.ProgressPhotoEntity
import com.example.tracker.domain.model.PhotoPose
import com.example.tracker.domain.model.ProgressPhoto
import com.example.tracker.domain.repository.ProgressPhotoRepository
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** Keeps photo rows in the database and their pictures in [store]. Nothing is synced. */
class LocalProgressPhotoRepository(
    database: TrackerDatabase,
    private val store: ProgressPhotoStore,
    private val time: TimeProvider,
    private val ids: IdGenerator,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ProgressPhotoRepository {

    private val photoDao = database.progressPhotoDao()

    override fun observePhotos(): Flow<List<ProgressPhoto>> = photoDao.observeAll().map { rows ->
        rows.sortedWith(compareByDescending<ProgressPhotoEntity> { it.epochDay }.thenBy { it.pose.ordinal })
            .map { it.toDomain() }
    }

    override suspend fun createCaptureFile(): File = withContext(ioDispatcher) { store.newCaptureFile() }

    override suspend fun savePhoto(date: LocalDate, pose: PhotoPose, capture: File) {
        require(capture.length() > 0) { "Empty capture: $capture" }
        val replaced = photoDao.get(date.toEpochDay(), pose.name)
        // A new name each time, so an image loader never shows a cached copy of the old picture.
        val id = ids.newId()
        val fileName = "$id.jpg"
        withContext(ioDispatcher) { store.keep(capture, fileName) }
        try {
            photoDao.upsert(
                ProgressPhotoEntity(
                    id = replaced?.id ?: id,
                    epochDay = date.toEpochDay(),
                    pose = pose,
                    fileName = fileName,
                    createdAt = time.now().toEpochMilli(),
                ),
            )
        } catch (e: Exception) {
            withContext(ioDispatcher) { store.delete(fileName) }
            throw e
        }
        replaced?.let { withContext(ioDispatcher) { store.delete(it.fileName) } }
    }

    override suspend fun discardCapture(capture: File) = withContext(ioDispatcher) { store.discard(capture) }

    override suspend fun deletePhoto(id: String) {
        val photo = photoDao.getById(id) ?: return
        photoDao.delete(id)
        withContext(ioDispatcher) { store.delete(photo.fileName) }
    }

    private fun ProgressPhotoEntity.toDomain() = ProgressPhoto(
        id = id,
        date = LocalDate.ofEpochDay(epochDay),
        pose = pose,
        filePath = store.file(fileName).absolutePath,
    )
}
