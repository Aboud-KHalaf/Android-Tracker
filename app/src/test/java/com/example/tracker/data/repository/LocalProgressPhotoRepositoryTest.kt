package com.example.tracker.data.repository

import com.example.tracker.data.local.ProgressPhotoStore
import com.example.tracker.data.local.TrackerDatabase
import com.example.tracker.domain.model.PhotoMeasurement
import com.example.tracker.domain.model.PhotoPose
import java.io.File
import java.time.Duration
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LocalProgressPhotoRepositoryTest {

    @get:Rule
    val folder = TemporaryFolder()

    private lateinit var database: TrackerDatabase
    private lateinit var photosDirectory: File
    private lateinit var store: ProgressPhotoStore
    private val time = FakeTimeProvider()
    private val ids = SequentialIds()
    private val today = LocalDate.of(2026, 9, 28)

    @Before
    fun setUp() {
        database = inMemoryDatabase()
        photosDirectory = File(folder.root, "photos")
        store = ProgressPhotoStore(photosDirectory, File(folder.root, "captures"))
    }

    @After
    fun tearDown() = database.close()

    private fun TestScope.repository() =
        LocalProgressPhotoRepository(database, store, time, ids, StandardTestDispatcher(testScheduler))

    private suspend fun LocalProgressPhotoRepository.capture(content: String): File =
        createCaptureFile().apply { writeText(content) }

    @Test
    fun savePhoto_withoutCheckIn_startsOneTodayAndMovesTheCapture() = runTest {
        val photos = repository()
        val capture = photos.capture("front")

        val checkInId = photos.savePhoto(null, PhotoPose.FRONT, capture)

        val checkIn = photos.observeCheckIns().first().single()
        assertEquals(checkInId, checkIn.id)
        assertEquals(today, checkIn.date)
        assertEquals(time.now(), checkIn.takenAt)
        val saved = checkIn.photos.getValue(PhotoPose.FRONT)
        assertEquals(checkInId, saved.checkInId)
        assertEquals("front", File(saved.filePath).readText())
        assertEquals(photosDirectory, File(saved.filePath).parentFile)
        assertFalse(capture.exists())
    }

    @Test
    fun savePhoto_samePoseInTheCheckIn_replacesThePhotoAndDeletesTheOldFile() = runTest {
        val photos = repository()
        val checkInId = photos.savePhoto(null, PhotoPose.SIDE, photos.capture("first"))
        val first = photos.observeCheckIns().first().single().photos.getValue(PhotoPose.SIDE)

        photos.savePhoto(checkInId, PhotoPose.SIDE, photos.capture("second"))

        val second = photos.observeCheckIns().first().single().photos.getValue(PhotoPose.SIDE)
        assertEquals(first.id, second.id)
        assertEquals("second", File(second.filePath).readText())
        assertFalse(File(first.filePath).exists())
    }

    @Test
    fun savePhoto_newCheckInTheSameDay_keepsTheEarlierOne() = runTest {
        val photos = repository()
        photos.savePhoto(null, PhotoPose.FRONT, photos.capture("morning"))
        time.advance(Duration.ofHours(3))

        photos.savePhoto(null, PhotoPose.FRONT, photos.capture("evening"))

        val checkIns = photos.observeCheckIns().first()
        assertEquals(2, checkIns.size)
        assertEquals(listOf("evening", "morning"), checkIns.map { File(it.photos.getValue(PhotoPose.FRONT).filePath).readText() })
    }

    @Test
    fun observeCheckIns_newestFirst_photosInPoseOrder() = runTest {
        val photos = repository()
        val first = photos.savePhoto(null, PhotoPose.BACK, photos.capture("b"))
        photos.savePhoto(first, PhotoPose.FRONT, photos.capture("f"))
        time.advance(Duration.ofDays(7))
        photos.savePhoto(null, PhotoPose.SIDE, photos.capture("s"))

        val checkIns = photos.observeCheckIns().first()

        assertEquals(listOf(today.plusDays(7), today), checkIns.map { it.date })
        assertEquals(listOf(PhotoPose.FRONT, PhotoPose.BACK), checkIns[1].photos.keys.toList())
    }

    @Test
    fun saveMeasurements_replacesTheCheckInsMeasurementsInOrder() = runTest {
        val photos = repository()
        val checkInId = photos.savePhoto(null, PhotoPose.FRONT, photos.capture("f"))
        photos.saveMeasurements(checkInId, listOf(PhotoMeasurement("Chest", "cm", 100.0)))

        photos.saveMeasurements(checkInId, listOf(PhotoMeasurement("Waist", "cm", 84.5), PhotoMeasurement("Weight", "kg", 78.4)))

        assertEquals(
            listOf(PhotoMeasurement("Waist", "cm", 84.5), PhotoMeasurement("Weight", "kg", 78.4)),
            photos.observeCheckIns().first().single().measurements,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun saveMeasurements_negativeValue_isRejected() = runTest {
        val photos = repository()
        val checkInId = photos.savePhoto(null, PhotoPose.FRONT, photos.capture("f"))
        photos.saveMeasurements(checkInId, listOf(PhotoMeasurement("Waist", "cm", -1.0)))
    }

    @Test
    fun deletePhoto_removesTheRowAndTheFile_andTheEmptyCheckIn() = runTest {
        val photos = repository()
        val checkInId = photos.savePhoto(null, PhotoPose.FRONT, photos.capture("front"))
        photos.savePhoto(checkInId, PhotoPose.BACK, photos.capture("back"))
        val front = photos.observeCheckIns().first().single().photos.getValue(PhotoPose.FRONT)

        photos.deletePhoto(front.id)

        assertEquals(setOf(PhotoPose.BACK), photos.observeCheckIns().first().single().photos.keys)
        assertFalse(File(front.filePath).exists())

        photos.deletePhoto(photos.observeCheckIns().first().single().photos.getValue(PhotoPose.BACK).id)
        assertTrue(photos.observeCheckIns().first().isEmpty())
    }

    @Test
    fun deleteCheckIn_removesItsPhotosFilesAndMeasurements() = runTest {
        val photos = repository()
        val checkInId = photos.savePhoto(null, PhotoPose.FRONT, photos.capture("front"))
        photos.saveMeasurements(checkInId, listOf(PhotoMeasurement("Waist", "cm", 84.5)))
        val front = photos.observeCheckIns().first().single().photos.getValue(PhotoPose.FRONT)

        photos.deleteCheckIn(checkInId)

        assertTrue(photos.observeCheckIns().first().isEmpty())
        assertFalse(File(front.filePath).exists())
    }

    @Test
    fun discardCapture_deletesTheFile() = runTest {
        val photos = repository()
        val capture = photos.capture("unused")

        photos.discardCapture(capture)

        assertFalse(capture.exists())
    }

    @Test(expected = IllegalArgumentException::class)
    fun savePhoto_emptyCapture_isRejected() = runTest {
        val photos = repository()
        photos.savePhoto(null, PhotoPose.FRONT, photos.createCaptureFile())
    }
}
