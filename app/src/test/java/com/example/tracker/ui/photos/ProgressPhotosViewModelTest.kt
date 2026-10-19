package com.example.tracker.ui.photos

import com.example.tracker.R
import com.example.tracker.domain.model.PhotoMeasurement
import com.example.tracker.domain.model.PhotoPose
import com.example.tracker.domain.photos.PhotoComparison
import com.example.tracker.testing.FakeProgressPhotoRepository
import com.example.tracker.testing.MainDispatcherRule
import com.example.tracker.testing.photoCheckIn
import java.io.IOException
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProgressPhotosViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val photos = FakeProgressPhotoRepository()
    private val day = LocalDate.of(2026, 10, 1)

    private fun TestScope.createViewModel(): ProgressPhotosViewModel {
        val viewModel = ProgressPhotosViewModel(photos)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return viewModel
    }

    private fun ProgressPhotosViewModel.success() = uiState.value as ProgressPhotosUiState.Success

    @Test
    fun uiState_listsCheckInsNewestFirstAndComparesFirstWithLatestFront() = runTest {
        val first = photoCheckIn("a", day, PhotoPose.FRONT, PhotoPose.SIDE)
        val latest = photoCheckIn("b", day.plusDays(14), PhotoPose.FRONT)
        photos.put(first, latest)

        val state = createViewModel().success()

        assertEquals(listOf("b", "a"), state.checkIns.map { it.checkIn.id })
        assertEquals(PhotoPose.FRONT, state.comparePose)
        assertEquals(PhotoComparison(first.photos.getValue(PhotoPose.FRONT), latest.photos.getValue(PhotoPose.FRONT)), state.comparison)
    }

    @Test
    fun uiState_marksCheckInsThatShareADay_andMeasurementChanges() = runTest {
        val waist = PhotoMeasurement("Waist", "cm", 84.0)
        photos.put(
            photoCheckIn("morning", day, PhotoPose.FRONT, measurements = listOf(PhotoMeasurement("Waist", "cm", 85.0))),
            photoCheckIn("evening", day, PhotoPose.FRONT, hoursLater = 9, measurements = listOf(waist)),
            photoCheckIn("week", day.plusDays(7), PhotoPose.FRONT),
        )

        val checkIns = createViewModel().success().checkIns

        assertEquals(listOf(false, true, true), checkIns.map { it.sharesDay })
        assertEquals(mapOf(waist to -1.0), checkIns[1].changes)
    }

    @Test
    fun onSelectComparePose_withOneCheckInOfThatPose_hasNoComparison() = runTest {
        photos.put(photoCheckIn("a", day, PhotoPose.FRONT, PhotoPose.SIDE), photoCheckIn("b", day.plusDays(7), PhotoPose.FRONT))
        val viewModel = createViewModel()

        viewModel.onSelectComparePose(PhotoPose.SIDE)

        assertEquals(PhotoPose.SIDE, viewModel.success().comparePose)
        assertNull(viewModel.success().comparison)
    }

    @Test
    fun uiState_empty_whenNoPhotos() = runTest {
        assertTrue(createViewModel().success().checkIns.isEmpty())
    }

    @Test
    fun uiState_error_thenRetryRecovers() = runTest {
        photos.observeError = IOException("disk")
        val viewModel = createViewModel()
        assertEquals(ProgressPhotosUiState.Error, viewModel.uiState.value)

        photos.observeError = null
        viewModel.onRetry()

        assertTrue(viewModel.uiState.value is ProgressPhotosUiState.Success)
    }

    @Test
    fun onDeletePhoto_removesItAndConfirms() = runTest {
        photos.put(photoCheckIn("a", day, PhotoPose.FRONT))
        val viewModel = ProgressPhotosViewModel(photos)

        viewModel.onDeletePhoto("a-FRONT")

        assertTrue(photos.checkIns.value.isEmpty())
        assertEquals(ProgressPhotosEvent.ShowMessage(R.string.photos_deleted), viewModel.events.first())
    }

    @Test
    fun onDeletePhoto_failure_showsError() = runTest {
        photos.put(photoCheckIn("a", day, PhotoPose.FRONT))
        photos.writeError = IOException("disk")
        val viewModel = ProgressPhotosViewModel(photos)

        viewModel.onDeletePhoto("a-FRONT")

        assertEquals(1, photos.checkIns.value.size)
        assertEquals(ProgressPhotosEvent.ShowMessage(R.string.photos_error_delete), viewModel.events.first())
    }

    @Test
    fun onDeleteCheckIn_removesItAndConfirms() = runTest {
        photos.put(photoCheckIn("a", day, PhotoPose.FRONT, PhotoPose.BACK))
        val viewModel = ProgressPhotosViewModel(photos)

        viewModel.onDeleteCheckIn("a")

        assertTrue(photos.checkIns.value.isEmpty())
        assertEquals(ProgressPhotosEvent.ShowMessage(R.string.photos_check_in_deleted), viewModel.events.first())
    }

    @Test
    fun editingMeasurements_savesThemWithTheCheckIn() = runTest {
        val checkIn = photoCheckIn("a", day, PhotoPose.FRONT, measurements = listOf(PhotoMeasurement("Weight", "kg", 80.0)))
        photos.put(checkIn)
        val viewModel = ProgressPhotosViewModel(photos)

        viewModel.onEditMeasurements(checkIn)
        viewModel.onMeasurementValueChange(0, "79.6")
        viewModel.onToggleMeasurement("Waist", "cm")
        viewModel.onMeasurementValueChange(1, "84")
        viewModel.onSaveMeasurements()

        assertNull(viewModel.measurements.value)
        assertEquals(
            listOf(PhotoMeasurement("Weight", "kg", 79.6), PhotoMeasurement("Waist", "cm", 84.0)),
            photos.checkIns.value.single().measurements,
        )
    }

    @Test
    fun editingMeasurements_invalidValue_keepsTheFormOpen() = runTest {
        val checkIn = photoCheckIn("a", day, PhotoPose.FRONT)
        photos.put(checkIn)
        val viewModel = ProgressPhotosViewModel(photos)
        viewModel.onEditMeasurements(checkIn)
        viewModel.onToggleMeasurement("Waist", "cm")
        viewModel.onMeasurementValueChange(0, ".")

        viewModel.onSaveMeasurements()

        assertTrue(viewModel.measurements.value!!.fields.single().error)
        assertFalse(viewModel.measurements.value!!.isSaving)
    }
}
