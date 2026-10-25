package com.example.tracker.ui.stats

import com.example.tracker.R
import com.example.tracker.data.repository.FakeTimeProvider
import com.example.tracker.domain.model.CustomStat
import com.example.tracker.domain.model.PhotoPose
import com.example.tracker.domain.model.StatColor
import com.example.tracker.domain.model.StatGoal
import com.example.tracker.domain.model.StatEntry
import com.example.tracker.domain.model.StatIcon
import com.example.tracker.testing.FakeCustomStatRepository
import com.example.tracker.testing.FakeProgressPhotoRepository
import com.example.tracker.testing.FakeSettingsRepository
import com.example.tracker.testing.MainDispatcherRule
import com.example.tracker.testing.photoCheckIn
import java.io.IOException
import java.time.Instant
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
class StatsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val settings = FakeSettingsRepository()
    private val photos = FakeProgressPhotoRepository()
    private val stats = FakeCustomStatRepository()
    private val time = FakeTimeProvider(Instant.parse("2026-10-03T09:00:00Z"))
    private val today = LocalDate.of(2026, 10, 3)

    private val bodyFat = CustomStat("fat", "Body fat", "%", 1, 15.0, StatIcon.PERCENT, StatColor.ORANGE)

    private fun createViewModel() = StatsViewModel(stats, photos, settings, time)

    private fun TestScope.collectState(viewModel: StatsViewModel): () -> StatsUiState {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return { viewModel.uiState.value }
    }

    @Test
    fun uiState_customStat_hasLatestChangeAndProgress() = runTest {
        stats.stats.value = listOf(bodyFat)
        stats.put(StatEntry("fat", today.minusDays(10), 21.0), StatEntry("fat", today, 18.0))
        val card = (collectState(createViewModel())() as StatsUiState.Success).stats.single()

        assertEquals(StatEntry("fat", today, 18.0), card.latest)
        assertEquals(-3.0, card.changeSinceStart!!, 1e-9)
        assertEquals(0.5f, card.progress!!, 1e-6f)
    }

    @Test
    fun uiState_dailyGoalPastItsTarget_isFullProgress() = runTest {
        val steps = CustomStat("steps", "Steps", "steps", 0, 100.0, StatIcon.STEPS, StatColor.TEAL, StatGoal.DAILY)
        stats.stats.value = listOf(steps)
        stats.put(StatEntry("steps", today, 110.0))
        val card = (collectState(createViewModel())() as StatsUiState.Success).stats.single()

        assertEquals(1f, card.progress!!, 0f)
    }

    @Test
    fun uiState_customStatWithoutEntriesOrTarget_hasNoChangeOrProgress() = runTest {
        stats.stats.value = listOf(bodyFat, bodyFat.copy(id = "waist", target = null))
        stats.put(StatEntry("waist", today, 84.0))
        val cards = (collectState(createViewModel())() as StatsUiState.Success).stats

        assertNull(cards[0].latest)
        assertNull(cards[0].progress)
        assertNull(cards[1].changeSinceStart)
        assertNull(cards[1].progress)
    }

    @Test
    fun uiState_errorWhenStatsFailToLoad() = runTest {
        stats.observeError = IOException("disk")

        assertEquals(StatsUiState.Error, collectState(createViewModel())())
    }

    @Test
    fun latestCheckIn_isTheNewestDayWithPhotos() = runTest {
        photos.put(
            photoCheckIn("old", today.minusDays(7), PhotoPose.FRONT),
            photoCheckIn("latest", today, PhotoPose.FRONT, PhotoPose.BACK),
        )
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.latestCheckIn.collect {} }

        val latest = viewModel.latestCheckIn.value

        assertEquals(today, latest?.date)
        assertEquals(setOf(PhotoPose.FRONT, PhotoPose.BACK), latest?.photos?.keys)
    }

    @Test
    fun latestCheckIn_nullWhenPhotosFailToLoad() = runTest {
        photos.observeError = IOException("disk")
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.latestCheckIn.collect {} }

        assertNull(viewModel.latestCheckIn.value)
    }

    @Test
    fun notificationPermission_isAskedOnceThereIsAStat() = runTest {
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.shouldRequestNotificationPermission.collect {} }
        assertFalse(viewModel.shouldRequestNotificationPermission.value)

        stats.stats.value = listOf(bodyFat)
        assertTrue(viewModel.shouldRequestNotificationPermission.value)

        viewModel.onNotificationPermissionRequested()
        assertFalse(viewModel.shouldRequestNotificationPermission.value)
    }

    @Test
    fun notificationPermission_notAskedWhileTheReminderIsOff() = runTest {
        stats.stats.value = listOf(bodyFat)
        settings.reminderEnabled.value = false
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.shouldRequestNotificationPermission.collect {} }

        assertFalse(viewModel.shouldRequestNotificationPermission.value)
    }

    @Test
    fun onDeleteStat_deletesItAndSaysSo() = runTest {
        stats.stats.value = listOf(bodyFat)
        val viewModel = createViewModel()
        val state = collectState(viewModel)

        viewModel.onDeleteStat("fat")

        assertTrue((state() as StatsUiState.Success).stats.isEmpty())
        assertEquals(StatsEvent.ShowMessage(R.string.stat_deleted), viewModel.events.first())
    }

    @Test
    fun onDeleteStat_failure_showsMessage() = runTest {
        stats.stats.value = listOf(bodyFat)
        stats.writeError = IOException("disk full")
        val viewModel = createViewModel()

        viewModel.onDeleteStat("fat")

        assertEquals(StatsEvent.ShowMessage(R.string.stat_error_delete_stat), viewModel.events.first())
        assertEquals(listOf(bodyFat), stats.stats.value)
    }
}
