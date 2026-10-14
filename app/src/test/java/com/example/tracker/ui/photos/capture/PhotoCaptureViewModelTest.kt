package com.example.tracker.ui.photos.capture

import com.example.tracker.R
import com.example.tracker.data.repository.FakeTimeProvider
import com.example.tracker.domain.model.PhotoPose
import com.example.tracker.domain.model.ProgressPhoto
import com.example.tracker.testing.FakeProgressPhotoRepository
import com.example.tracker.testing.MainDispatcherRule
import java.io.File
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PhotoCaptureViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val photos = FakeProgressPhotoRepository()
    private val time = FakeTimeProvider(Instant.parse("2026-10-03T09:00:00Z"))
    private val today = LocalDate.of(2026, 10, 3)

    private fun TestScope.createViewModel(): PhotoCaptureViewModel {
        val viewModel = PhotoCaptureViewModel(photos, time)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return viewModel
    }

    /** Presses the shutter with the timer off and plays the camera's part: it writes the picture. */
    private suspend fun TestScope.takePhoto(viewModel: PhotoCaptureViewModel): File {
        while (viewModel.uiState.value.timer != CaptureTimer.OFF) viewModel.onCycleTimer()
        viewModel.onShutter()
        runCurrent()
        val event = viewModel.events.first() as PhotoCaptureEvent.TakePicture
        event.file.writeText("jpeg")
        viewModel.onPictureTaken(event.file)
        return event.file
    }

    @Test
    fun startsOnFrontWithTenSecondTimerAndBackCamera() = runTest {
        val state = createViewModel().uiState.value

        assertEquals(PhotoPose.FRONT, state.pose)
        assertEquals(CaptureTimer.TEN, state.timer)
        assertFalse(state.useFrontCamera)
        assertNull(state.guidePhotoPath)
    }

    @Test
    fun guidePhoto_isTheLastEarlierPhotoOfThePose() = runTest {
        photos.put(
            ProgressPhoto("f-old", today.minusDays(14), PhotoPose.FRONT, "/f-old.jpg"),
            ProgressPhoto("f-last", today.minusDays(7), PhotoPose.FRONT, "/f-last.jpg"),
            ProgressPhoto("s-last", today.minusDays(7), PhotoPose.SIDE, "/s-last.jpg"),
        )
        val viewModel = createViewModel()

        assertEquals("/f-last.jpg", viewModel.uiState.value.guidePhotoPath)
        viewModel.onSelectPose(PhotoPose.SIDE)
        assertEquals("/s-last.jpg", viewModel.uiState.value.guidePhotoPath)
        viewModel.onSelectPose(PhotoPose.BACK)
        assertNull(viewModel.uiState.value.guidePhotoPath)
    }

    @Test
    fun timer_countsDownThenTakesThePicture() = runTest {
        val viewModel = createViewModel()

        viewModel.onShutter()
        runCurrent()
        assertEquals(10, viewModel.uiState.value.countdown)
        advanceTimeBy(9_000)
        runCurrent()
        assertEquals(1, viewModel.uiState.value.countdown)
        advanceTimeBy(1_000)
        runCurrent()

        assertNull(viewModel.uiState.value.countdown)
        assertTrue(viewModel.uiState.value.isTakingPicture)
        assertTrue(viewModel.events.first() is PhotoCaptureEvent.TakePicture)
    }

    @Test
    fun shutterDuringCountdown_stopsTheTimer() = runTest {
        val viewModel = createViewModel()
        viewModel.onShutter()
        runCurrent()

        viewModel.onShutter()
        advanceTimeBy(20_000)
        runCurrent()

        assertNull(viewModel.uiState.value.countdown)
        assertFalse(viewModel.uiState.value.isTakingPicture)
    }

    @Test
    fun takenPicture_isReviewedThenSavedForTodayAndMovesToTheNextPose() = runTest {
        val viewModel = createViewModel()
        val capture = takePhoto(viewModel)
        assertEquals(capture.absolutePath, viewModel.uiState.value.reviewPath)

        viewModel.onUsePhoto()

        val saved = photos.photos.value.single()
        assertEquals(today, saved.date)
        assertEquals(PhotoPose.FRONT, saved.pose)
        val state = viewModel.uiState.value
        assertNull(state.reviewPath)
        assertEquals(PhotoPose.SIDE, state.pose)
        assertEquals(setOf(PhotoPose.FRONT), state.takenToday)
    }

    @Test
    fun retake_discardsThePicture() = runTest {
        val viewModel = createViewModel()
        val capture = takePhoto(viewModel)

        viewModel.onRetake()

        assertNull(viewModel.uiState.value.reviewPath)
        assertEquals(listOf(capture), photos.discarded)
        assertTrue(photos.photos.value.isEmpty())
    }

    @Test
    fun savingTheLastMissingPose_finishes() = runTest {
        photos.put(
            ProgressPhoto("f", today, PhotoPose.FRONT, "/f.jpg"),
            ProgressPhoto("s", today, PhotoPose.SIDE, "/s.jpg"),
        )
        val viewModel = createViewModel()
        viewModel.onSelectPose(PhotoPose.BACK)
        takePhoto(viewModel)

        viewModel.onUsePhoto()

        assertEquals(PhotoCaptureEvent.Finished, viewModel.events.first())
    }

    @Test
    fun failedPicture_showsErrorAndDiscardsTheFile() = runTest {
        val viewModel = createViewModel()
        while (viewModel.uiState.value.timer != CaptureTimer.OFF) viewModel.onCycleTimer()
        viewModel.onShutter()
        runCurrent()
        val event = viewModel.events.first() as PhotoCaptureEvent.TakePicture

        viewModel.onPictureFailed(event.file)

        assertFalse(viewModel.uiState.value.isTakingPicture)
        assertEquals(listOf(event.file), photos.discarded)
        assertEquals(PhotoCaptureEvent.ShowMessage(R.string.capture_error_take), viewModel.events.first())
    }

    @Test
    fun saveFailure_keepsTheReviewAndShowsError() = runTest {
        val viewModel = createViewModel()
        takePhoto(viewModel)
        photos.writeError = IOException("disk full")

        viewModel.onUsePhoto()

        val state = viewModel.uiState.value
        assertTrue(state.reviewPath != null)
        assertFalse(state.isSaving)
        assertEquals(PhotoCaptureEvent.ShowMessage(R.string.capture_error_save), viewModel.events.first())
    }

    @Test
    fun nextPose_wrapsAroundAndSkipsTakenPoses() {
        assertEquals(PhotoPose.FRONT, PhotoCaptureViewModel.nextPose(PhotoPose.BACK, setOf(PhotoPose.BACK)))
        assertEquals(PhotoPose.BACK, PhotoCaptureViewModel.nextPose(PhotoPose.FRONT, setOf(PhotoPose.FRONT, PhotoPose.SIDE)))
        assertNull(PhotoCaptureViewModel.nextPose(PhotoPose.SIDE, PhotoPose.entries.toSet()))
    }
}
