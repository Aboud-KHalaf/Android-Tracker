package com.example.tracker.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.tracker.TrackerApplication
import com.example.tracker.core.TimeProvider
import com.example.tracker.domain.model.StatOverview
import com.example.tracker.domain.photos.PhotoCheckIn
import com.example.tracker.domain.repository.CustomStatRepository
import com.example.tracker.domain.repository.NutritionRepository
import com.example.tracker.domain.repository.ProgressPhotoRepository
import com.example.tracker.domain.repository.SettingsRepository
import com.example.tracker.domain.repository.WeightRepository
import com.example.tracker.domain.stats.progressToward
import com.example.tracker.domain.weight.WeightSummary
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/** Stats: weight and the daily log as built-in stats, the latest progress photos, and the user's own stats. */
@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModel(
    private val weightRepository: WeightRepository,
    private val nutritionRepository: NutritionRepository,
    private val settingsRepository: SettingsRepository,
    photoRepository: ProgressPhotoRepository,
    private val customStatRepository: CustomStatRepository,
    private val time: TimeProvider,
) : ViewModel() {

    private val loadAttempt = MutableStateFlow(0)

    val uiState: StateFlow<StatsUiState> = loadAttempt
        .flatMapLatest {
            val today = today()
            combine(
                weightRepository.observeLatest(),
                weightRepository.observeEntries(today.minusDays(RECENT_DAYS - 1), today),
                nutritionRepository.observeDay(today),
                settingsRepository.nutritionTargets,
                customStatRepository.observeOverviews(),
            ) { latestWeight, recentWeights, todayNutrition, targets, overviews ->
                StatsUiState.Success(
                    today = today,
                    weight = WeightOverviewUi(
                        latest = latestWeight,
                        recentChangeKg = WeightSummary.of(recentWeights)?.takeIf { it.entryCount > 1 }?.changeKg,
                    ),
                    daily = DailyOverviewUi(todayNutrition, targets),
                    stats = overviews.map { it.toCardUi() },
                ) as StatsUiState
            }
                .onStart { emit(StatsUiState.Loading) }
                .catch { emit(StatsUiState.Error) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), StatsUiState.Loading)

    /** The latest photo check-in, or null before the first; photos failing to load just hide it. */
    val latestCheckIn: StateFlow<PhotoCheckIn?> = photoRepository.observeCheckIns()
        .map { it.firstOrNull() }
        .catch { emit(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    fun onRetry() {
        loadAttempt.update { it + 1 }
    }

    private fun today(): LocalDate = time.now().atZone(time.zone()).toLocalDate()

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        /** The weight card's change covers this many days, today included. */
        internal const val RECENT_DAYS = 30L

        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as TrackerApplication).container
                StatsViewModel(
                    container.weightRepository,
                    container.nutritionRepository,
                    container.settingsRepository,
                    container.progressPhotoRepository,
                    container.customStatRepository,
                    container.time,
                )
            }
        }
    }
}

internal fun StatOverview.toCardUi(): StatCardUi {
    val first = first
    val latest = latest
    return StatCardUi(
        stat = stat,
        latest = latest,
        changeSinceStart = if (first != null && latest != null && first.date != latest.date) latest.value - first.value else null,
        progress = stat.target?.let { target ->
            if (first != null && latest != null) progressToward(target, first.value, latest.value) else null
        },
    )
}
