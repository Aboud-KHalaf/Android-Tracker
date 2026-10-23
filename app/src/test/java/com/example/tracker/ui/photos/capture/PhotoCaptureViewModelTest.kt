package com.example.tracker.ui.photos.capture

import com.example.tracker.R
import com.example.tracker.domain.model.PhotoPose
import com.example.tracker.domain.model.PhotoMeasurement
import com.example.tracker.testing.FakeProgressPhotoRepository
import com.example.tracker.testing.MainDispatcherRule
import com.example.tracker.testing.photoCheckIn
import com.example.tracker.ui.photos.measurements.MeasurementField
import java.io.File
import java.io.IOException
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
    private val today = LocalDate.of(2026, 10, 3)

    private fun TestScope.createViewModel(): PhotoCaptureViewModel {
        val viewModel = PhotoCaptureViewModel(photos)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return viewModel
    }

    /** Presses the shutter with the timer off and plays the camera's part: it writes the picture. */
    private suspend fun TestScope.takePhoto(viewModel: PhotoCaptureViewModel): File {
        viewModel.onSelectTimer(CaptureTimer.OFF)
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
            photoCheckIn("old", today.minusDays(14), PhotoPose.FRONT),
            photoCheckIn("last", today.minusDays(7), PhotoPose.FRONT, PhotoPose.SIDE),
        )
        val viewModel = createViewModel()

        assertEquals("/last-FRONT.jpg", viewModel.uiState.value.guidePhotoPath)
        viewModel.onSelectPose(PhotoPose.SIDE)
        assertEquals("/last-SIDE.jpg", viewModel.uiState.value.guidePhotoPath)
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
    fun selectedTimer_countsDownItsSeconds() = runTest {
        val viewModel = createViewModel()

        viewModel.onSelectTimer(CaptureTimer.FIVE)
        viewModel.onShutter()
        runCurrent()

        assertEquals(CaptureTimer.FIVE, viewModel.uiState.value.timer)
        assertEquals(5, viewModel.uiState.value.countdown)
        // The length can't change mid-countdown.
        viewModel.onSelectTimer(CaptureTimer.THREE)
        assertEquals(CaptureTimer.FIVE, viewModel.uiState.value.timer)
        advanceTimeBy(5_000)
        runCurrent()
        assertTrue(viewModel.uiState.value.isTakingPicture)
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
    fun takenPicture_isReviewedThenSavedInANewCheckInAndMovesToTheNextPose() = runTest {
        val viewModel = createViewModel()
        val capture = takePhoto(viewModel)
        assertEquals(capture.absolutePath, viewModel.uiState.value.reviewPath)

        viewModel.onUsePhoto()

        val checkIn = photos.checkIns.value.single()
        assertEquals(today, checkIn.date)
        assertEquals(setOf(PhotoPose.FRONT), checkIn.photos.keys)
        val state = viewModel.uiState.value
        assertEquals(checkIn.id, state.checkInId)
        assertNull(state.reviewPath)
        assertEquals(PhotoPose.SIDE, state.pose)
        assertEquals(setOf(PhotoPose.FRONT), state.taken)
    }

    @Test
    fun aSecondSessionTheSameDay_keepsTheFirst() = runTest {
        photos.put(photoCheckIn("morning", today, *PhotoPose.entries.toTypedArray()))
        val viewModel = createViewModel()
        assertTrue(viewModel.uiState.value.taken.isEmpty())

        takePhoto(viewModel)
        viewModel.onUsePhoto()

        assertEquals(2, photos.checkIns.value.size)
        assertEquals(PhotoPose.entries.toSet(), photos.checkIns.value.first { it.id == "morning" }.photos.keys)
        assertEquals("/morning-SIDE.jpg", viewModel.uiState.value.guidePhotoPath)
    }

    @Test
    fun retake_discardsThePicture() = runTest {
        val viewModel = createViewModel()
        val capture = takePhoto(viewModel)

        viewModel.onRetake()

        assertNull(viewModel.uiState.value.reviewPath)
        assertEquals(listOf(capture), photos.discarded)
        assertTrue(photos.checkIns.value.isEmpty())
    }

    @Test
    fun savingTheLastPose_asksForMeasurements_startingWithLastTimes() = runTest {
        photos.put(
            photoCheckIn(
                "last",
                today.minusDays(7),
                PhotoPose.FRONT,
                measurements = listOf(PhotoMeasurement("Waist", "cm", 86.0)),
            ),
        )
        val viewModel = createViewModel()
        repeat(PhotoPose.entries.size) {
            takePhoto(viewModel)
            viewModel.onUsePhoto()
        }

        val form = viewModel.uiState.value.measurements!!
        assertEquals(viewModel.uiState.value.checkInId, form.checkInId)
        assertEquals(listOf(MeasurementField("Waist", "cm")), form.fields)
    }

    @Test
    fun finishingEarly_asksForMeasurements() = runTest {
        val viewModel = createViewModel()
        assertFalse(viewModel.uiState.value.canFinish)
        takePhoto(viewModel)
        viewModel.onUsePhoto()

        viewModel.onFinishPhotos()

        assertTrue(viewModel.uiState.value.measurements!!.fields.isEmpty())
    }

    @Test
    fun savingMeasurements_keepsThemWithTheCheckInAndFinishes() = runTest {
        val viewModel = createViewModel()
        takePhoto(viewModel)
        viewModel.onUsePhoto()
        viewModel.onFinishPhotos()

        viewModel.onToggleMeasurement("Waist", "cm")
        viewModel.onAddMeasurement("Biceps", "cm")
        viewModel.onMeasurementValueChange(0, "84,5")
        viewModel.onSaveMeasurements()

        assertEquals(PhotoCaptureEvent.Finished, viewModel.events.first())
        assertEquals(listOf(PhotoMeasurement("Waist", "cm", 84.5)), photos.checkIns.value.single().measurements)
    }

    @Test
    fun skippingMeasurements_finishesWithoutThem() = runTest {
        val viewModel = createViewModel()
        takePhoto(viewModel)
        viewModel.onUsePhoto()
        viewModel.onFinishPhotos()

        viewModel.onSkipMeasurements()

        assertEquals(PhotoCaptureEvent.Finished, viewModel.events.first())
        assertTrue(photos.checkIns.value.single().measurements.isEmpty())
    }

    @Test
    fun failedPicture_showsErrorAndDiscardsTheFile() = runTest {
        val viewModel = createViewModel()
        viewModel.onSelectTimer(CaptureTimer.OFF)
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
