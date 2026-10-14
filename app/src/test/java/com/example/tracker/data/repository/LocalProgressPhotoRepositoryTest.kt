package com.example.tracker.data.repository

import com.example.tracker.data.local.ProgressPhotoStore
import com.example.tracker.data.local.TrackerDatabase
import com.example.tracker.domain.model.PhotoPose
import java.io.File
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
    private val day = LocalDate.of(2026, 10, 1)

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
    fun savePhoto_movesTheCaptureIntoThePhotosFolder() = runTest {
        val photos = repository()
        val capture = photos.capture("front")

        photos.savePhoto(day, PhotoPose.FRONT, capture)

        val saved = photos.observePhotos().first().single()
        assertEquals(day, saved.date)
        assertEquals(PhotoPose.FRONT, saved.pose)
        assertEquals("front", File(saved.filePath).readText())
        assertEquals(photosDirectory, File(saved.filePath).parentFile)
        assertFalse(capture.exists())
    }

    @Test
    fun savePhoto_sameDayAndPose_replacesThePhotoAndDeletesTheOldFile() = runTest {
        val photos = repository()
        photos.savePhoto(day, PhotoPose.SIDE, photos.capture("first"))
        val first = photos.observePhotos().first().single()

        photos.savePhoto(day, PhotoPose.SIDE, photos.capture("second"))

        val second = photos.observePhotos().first().single()
        assertEquals(first.id, second.id)
        assertEquals("second", File(second.filePath).readText())
        assertFalse(File(first.filePath).exists())
    }

    @Test
    fun observePhotos_newestDayFirstThenPoseOrder() = runTest {
        val photos = repository()
        photos.savePhoto(day, PhotoPose.BACK, photos.capture("b"))
        photos.savePhoto(day, PhotoPose.FRONT, photos.capture("f"))
        photos.savePhoto(day.plusDays(7), PhotoPose.SIDE, photos.capture("s"))

        val order = photos.observePhotos().first().map { it.date to it.pose }

        assertEquals(
            listOf(day.plusDays(7) to PhotoPose.SIDE, day to PhotoPose.FRONT, day to PhotoPose.BACK),
            order,
        )
    }

    @Test
    fun deletePhoto_removesTheRowAndTheFile() = runTest {
        val photos = repository()
        photos.savePhoto(day, PhotoPose.FRONT, photos.capture("front"))
        val saved = photos.observePhotos().first().single()

        photos.deletePhoto(saved.id)

        assertTrue(photos.observePhotos().first().isEmpty())
        assertFalse(File(saved.filePath).exists())
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
        photos.savePhoto(day, PhotoPose.FRONT, photos.createCaptureFile())
    }
}
