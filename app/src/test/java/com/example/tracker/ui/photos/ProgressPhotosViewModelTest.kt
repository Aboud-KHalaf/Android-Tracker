package com.example.tracker.ui.photos

import com.example.tracker.R
import com.example.tracker.domain.model.PhotoPose
import com.example.tracker.domain.model.ProgressPhoto
import com.example.tracker.domain.photos.PhotoComparison
import com.example.tracker.testing.FakeProgressPhotoRepository
import com.example.tracker.testing.MainDispatcherRule
import java.io.IOException
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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

    private fun photo(id: String, date: LocalDate, pose: PhotoPose) = ProgressPhoto(id, date, pose, "/$id.jpg")

    @Test
    fun uiState_groupsDaysAndComparesFirstWithLatestFront() = runTest {
        val first = photo("f1", day, PhotoPose.FRONT)
        val latest = photo("f2", day.plusDays(14), PhotoPose.FRONT)
        photos.put(first, photo("s1", day, PhotoPose.SIDE), latest)
        val viewModel = ProgressPhotosViewModel(photos)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        val state = viewModel.uiState.value as ProgressPhotosUiState.Success

        assertEquals(listOf(day.plusDays(14), day), state.checkIns.map { it.date })
        assertEquals(PhotoPose.FRONT, state.comparePose)
        assertEquals(PhotoComparison(first, latest), state.comparison)
    }

    @Test
    fun onSelectComparePose_withOneDayOfThatPose_hasNoComparison() = runTest {
        photos.put(photo("f1", day, PhotoPose.FRONT), photo("f2", day.plusDays(7), PhotoPose.FRONT), photo("s1", day, PhotoPose.SIDE))
        val viewModel = ProgressPhotosViewModel(photos)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.onSelectComparePose(PhotoPose.SIDE)

        val state = viewModel.uiState.value as ProgressPhotosUiState.Success
        assertEquals(PhotoPose.SIDE, state.comparePose)
        assertNull(state.comparison)
    }

    @Test
    fun uiState_empty_whenNoPhotos() = runTest {
        val viewModel = ProgressPhotosViewModel(photos)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        assertTrue((viewModel.uiState.value as ProgressPhotosUiState.Success).checkIns.isEmpty())
    }

    @Test
    fun uiState_error_thenRetryRecovers() = runTest {
        photos.observeError = IOException("disk")
        val viewModel = ProgressPhotosViewModel(photos)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        assertEquals(ProgressPhotosUiState.Error, viewModel.uiState.value)

        photos.observeError = null
        viewModel.onRetry()

        assertTrue(viewModel.uiState.value is ProgressPhotosUiState.Success)
    }

    @Test
    fun onDeletePhoto_removesItAndConfirms() = runTest {
        photos.put(photo("f1", day, PhotoPose.FRONT))
        val viewModel = ProgressPhotosViewModel(photos)

        viewModel.onDeletePhoto("f1")

        assertTrue(photos.photos.value.isEmpty())
        assertEquals(ProgressPhotosEvent.ShowMessage(R.string.photos_deleted), viewModel.events.first())
    }

    @Test
    fun onDeletePhoto_failure_showsError() = runTest {
        photos.put(photo("f1", day, PhotoPose.FRONT))
        photos.writeError = IOException("disk")
        val viewModel = ProgressPhotosViewModel(photos)

        viewModel.onDeletePhoto("f1")

        assertEquals(1, photos.photos.value.size)
        assertEquals(ProgressPhotosEvent.ShowMessage(R.string.photos_error_delete), viewModel.events.first())
    }
}
