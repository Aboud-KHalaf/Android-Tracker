package com.example.tracker.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.tracker.R
import com.example.tracker.TrackerApplication
import com.example.tracker.core.TimeProvider
import com.example.tracker.domain.model.StatOverview
import com.example.tracker.domain.photos.PhotoCheckIn
import com.example.tracker.domain.repository.CustomStatRepository
import com.example.tracker.domain.repository.ProgressPhotoRepository
import com.example.tracker.domain.repository.SettingsRepository
import com.example.tracker.domain.stats.progressToward
import com.example.tracker.ui.common.launchCatching
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * Stats: the user's stats with their latest values, suggestions for new ones, and the latest
 * progress photos. Messages are sent once through [events].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModel(
    private val customStatRepository: CustomStatRepository,
    photoRepository: ProgressPhotoRepository,
    private val settingsRepository: SettingsRepository,
    private val time: TimeProvider,
) : ViewModel() {

    private val loadAttempt = MutableStateFlow(0)

    private val _events = Channel<StatsEvent>(Channel.BUFFERED)
    val events: Flow<StatsEvent> = _events.receiveAsFlow()

    val uiState: StateFlow<StatsUiState> = loadAttempt
        .flatMapLatest {
            val today = today()
            customStatRepository.observeOverviews()
                .map { overviews -> StatsUiState.Success(today, overviews.map { it.toCardUi() }) as StatsUiState }
                .onStart { emit(StatsUiState.Loading) }
                .catch { emit(StatsUiState.Error) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), StatsUiState.Loading)

    /** The latest photo check-in, or null before the first; photos failing to load just hide it. */
    val latestCheckIn: StateFlow<PhotoCheckIn?> = photoRepository.observeCheckIns()
        .map { it.firstOrNull() }
        .catch { emit(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    /**
     * Whether to ask for notification permission now: once, while the daily reminder is on and
     * there is a stat for it to remind about. The screen checks whether it is actually missing.
     */
    val shouldRequestNotificationPermission: StateFlow<Boolean> = combine(
        settingsRepository.reminderEnabled,
        settingsRepository.notificationPermissionRequested,
        uiState,
    ) { enabled, requested, state ->
        enabled && !requested && state is StatsUiState.Success && state.stats.isNotEmpty()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), false)

    fun onNotificationPermissionRequested() {
        viewModelScope.launchCatching(onError = {}) { settingsRepository.markNotificationPermissionRequested() }
    }

    fun onRetry() {
        loadAttempt.update { it + 1 }
    }

    /** Deletes a stat and everything logged for it, after the screen has asked. */
    fun onDeleteStat(statId: String) {
        viewModelScope.launchCatching(onError = { _events.send(StatsEvent.ShowMessage(R.string.stat_error_delete_stat)) }) {
            customStatRepository.deleteStat(statId)
            _events.send(StatsEvent.ShowMessage(R.string.stat_deleted))
        }
    }

    private fun today(): LocalDate = time.now().atZone(time.zone()).toLocalDate()

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as TrackerApplication).container
                StatsViewModel(
                    container.customStatRepository,
                    container.progressPhotoRepository,
                    container.settingsRepository,
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
            if (first != null && latest != null) progressToward(stat.goal, target, first.value, latest.value) else null
        },
    )
}
