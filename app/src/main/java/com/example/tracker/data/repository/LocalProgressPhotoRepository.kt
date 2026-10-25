package com.example.tracker.data.repository

import com.example.tracker.core.IdGenerator
import com.example.tracker.core.TimeProvider
import com.example.tracker.data.local.ProgressPhotoStore
import com.example.tracker.data.local.TrackerDatabase
import com.example.tracker.data.local.entity.PhotoMeasurementEntity
import com.example.tracker.data.local.entity.PhotoSessionEntity
import com.example.tracker.data.local.entity.ProgressPhotoEntity
import com.example.tracker.domain.model.PhotoMeasurement
import com.example.tracker.domain.model.PhotoPose
import com.example.tracker.domain.model.ProgressPhoto
import com.example.tracker.domain.photos.PhotoCheckIn
import com.example.tracker.domain.repository.ProgressPhotoRepository
import java.io.File
import java.io.InputStream
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext

/** Keeps photo sessions in the database and their pictures in [store]. Nothing is synced. */
class LocalProgressPhotoRepository(
    database: TrackerDatabase,
    private val store: ProgressPhotoStore,
    private val time: TimeProvider,
    private val ids: IdGenerator,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ProgressPhotoRepository {

    private val photoDao = database.progressPhotoDao()

    override fun observeCheckIns(): Flow<List<PhotoCheckIn>> = combine(
        photoDao.observeSessions(),
        photoDao.observePhotos(),
        photoDao.observeMeasurements(),
    ) { sessions, photos, measurements ->
        val photosBySession = photos.groupBy { it.sessionId }
        val measurementsBySession = measurements.groupBy { it.sessionId }
        sessions.map { session ->
            val date = LocalDate.ofEpochDay(session.epochDay)
            PhotoCheckIn(
                id = session.id,
                date = date,
                takenAt = Instant.ofEpochMilli(session.takenAt),
                photos = photosBySession[session.id].orEmpty()
                    .sortedBy { it.pose.ordinal }
                    .associate { it.pose to it.toDomain(date) },
                measurements = measurementsBySession[session.id].orEmpty()
                    .sortedBy { it.position }
                    .map { PhotoMeasurement(it.name, it.unit, it.value) },
            )
        }
    }

    override suspend fun createCaptureFile(): File = withContext(ioDispatcher) { store.newCaptureFile() }

    // Whatever fails, the partial copy goes before the failure propagates.
    @Suppress("TooGenericExceptionCaught")
    override suspend fun importCapture(open: () -> InputStream): File = withContext(ioDispatcher) {
        val capture = store.newCaptureFile()
        try {
            open().use { input -> capture.outputStream().use { input.copyTo(it) } }
            require(capture.length() > 0) { "Nothing to import" }
            capture
        } catch (e: Throwable) {
            store.discard(capture)
            throw e
        }
    }

    // Whatever fails, the copied file goes before the failure propagates.
    @Suppress("TooGenericExceptionCaught")
    override suspend fun savePhoto(checkInId: String?, pose: PhotoPose, capture: File): String {
        require(capture.length() > 0) { "Empty capture: $capture" }
        val sessionId = checkInId?.also { requireNotNull(photoDao.getSession(it)) { "No check-in $it" } }
            ?: newSession()
        val replaced = photoDao.getPhoto(sessionId, pose.name)
        // A new name each time, so an image loader never shows a cached copy of the old picture.
        val id = ids.newId()
        val fileName = "$id.jpg"
        withContext(ioDispatcher) { store.keep(capture, fileName) }
        try {
            photoDao.upsertPhoto(
                ProgressPhotoEntity(
                    id = replaced?.id ?: id,
                    sessionId = sessionId,
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
        return sessionId
    }

    private suspend fun newSession(): String {
        val now = time.now()
        val session = PhotoSessionEntity(
            id = ids.newId(),
            epochDay = now.atZone(time.zone()).toLocalDate().toEpochDay(),
            takenAt = now.toEpochMilli(),
        )
        photoDao.insertSession(session)
        return session.id
    }

    override suspend fun saveMeasurements(checkInId: String, measurements: List<PhotoMeasurement>) {
        measurements.forEach { measurement ->
            require(measurement.name.isNotBlank() && measurement.name.length <= PhotoMeasurement.MAX_NAME_LENGTH) {
                "Invalid name: ${measurement.name}"
            }
            require(measurement.unit.length <= PhotoMeasurement.MAX_UNIT_LENGTH) { "Invalid unit: ${measurement.unit}" }
            require(measurement.value in PhotoMeasurement.MIN_VALUE..PhotoMeasurement.MAX_VALUE) {
                "Value out of range: ${measurement.value}"
            }
        }
        requireNotNull(photoDao.getSession(checkInId)) { "No check-in $checkInId" }
        photoDao.replaceMeasurements(
            checkInId,
            measurements.mapIndexed { i, m -> PhotoMeasurementEntity(checkInId, i, m.name.trim(), m.unit.trim(), m.value) },
        )
    }

    override suspend fun discardCapture(capture: File) = withContext(ioDispatcher) { store.discard(capture) }

    override suspend fun deletePhoto(id: String) {
        val photo = photoDao.getPhotoById(id) ?: return
        photoDao.deletePhoto(id)
        if (photoDao.photosOf(photo.sessionId).isEmpty()) photoDao.deleteSession(photo.sessionId)
        withContext(ioDispatcher) { store.delete(photo.fileName) }
    }

    override suspend fun deleteCheckIn(id: String) {
        val photos = photoDao.photosOf(id)
        photoDao.deleteSession(id)
        withContext(ioDispatcher) { photos.forEach { store.delete(it.fileName) } }
    }

    private fun ProgressPhotoEntity.toDomain(date: LocalDate) = ProgressPhoto(
        id = id,
        checkInId = sessionId,
        date = date,
        pose = pose,
        filePath = store.file(fileName).absolutePath,
    )
}
