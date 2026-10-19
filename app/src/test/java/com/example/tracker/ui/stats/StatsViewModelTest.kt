package com.example.tracker.ui.stats

import com.example.tracker.data.repository.FakeTimeProvider
import com.example.tracker.domain.model.CustomStat
import com.example.tracker.domain.model.DailyNutrition
import com.example.tracker.domain.model.NutritionTargets
import com.example.tracker.domain.model.PhotoPose
import com.example.tracker.domain.model.ProgressPhoto
import com.example.tracker.domain.model.StatColor
import com.example.tracker.domain.model.StatEntry
import com.example.tracker.domain.model.StatIcon
import com.example.tracker.domain.model.WeightEntry
import com.example.tracker.testing.FakeCustomStatRepository
import com.example.tracker.testing.FakeNutritionRepository
import com.example.tracker.testing.FakeProgressPhotoRepository
import com.example.tracker.testing.FakeSettingsRepository
import com.example.tracker.testing.FakeWeightRepository
import com.example.tracker.testing.MainDispatcherRule
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val weights = FakeWeightRepository()
    private val nutrition = FakeNutritionRepository()
    private val settings = FakeSettingsRepository()
    private val photos = FakeProgressPhotoRepository()
    private val stats = FakeCustomStatRepository()
    private val time = FakeTimeProvider(Instant.parse("2026-10-03T09:00:00Z"))
    private val today = LocalDate.of(2026, 10, 3)

    private val bodyFat = CustomStat("fat", "Body fat", "%", 1, 15.0, StatIcon.PERCENT, StatColor.ORANGE)

    private fun createViewModel() = StatsViewModel(weights, nutrition, settings, photos, stats, time)

    private fun TestScope.collectState(viewModel: StatsViewModel): () -> StatsUiState {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return { viewModel.uiState.value }
    }

    @Test
    fun uiState_showsWeightWithItsChangeOverThirtyDays() = runTest {
        weights.put(
            WeightEntry(today, 78.4),
            WeightEntry(today.minusDays(20), 80.0),
            WeightEntry(today.minusDays(60), 85.0), // before the 30 days
        )
        val state = collectState(createViewModel())() as StatsUiState.Success

        assertEquals(WeightEntry(today, 78.4), state.weight.latest)
        assertEquals(-1.6, state.weight.recentChangeKg!!, 1e-9)
    }

    @Test
    fun uiState_singleRecentWeight_hasNoChange() = runTest {
        weights.put(WeightEntry(today.minusDays(60), 85.0), WeightEntry(today, 78.4))
        val state = collectState(createViewModel())() as StatsUiState.Success

        assertNull(state.weight.recentChangeKg)
    }

    @Test
    fun uiState_showsTodaysDailyLogAgainstTargets() = runTest {
        nutrition.put(DailyNutrition(today, 1850, 120, 8000))
        settings.nutritionTargets.value = NutritionTargets(calories = 2300)
        val state = collectState(createViewModel())() as StatsUiState.Success

        assertEquals(DailyNutrition(today, 1850, 120, 8000), state.daily.today)
        assertEquals(2300, state.daily.targets.calories)
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
            ProgressPhoto("old", today.minusDays(7), PhotoPose.FRONT, "/old.jpg"),
            ProgressPhoto("front", today, PhotoPose.FRONT, "/front.jpg"),
            ProgressPhoto("back", today, PhotoPose.BACK, "/back.jpg"),
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
}
