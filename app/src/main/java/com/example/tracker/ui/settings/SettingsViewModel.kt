package com.example.tracker.ui.settings

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.tracker.R
import com.example.tracker.TrackerApplication
import com.example.tracker.domain.model.ThemeMode
import com.example.tracker.domain.repository.SettingsRepository
import com.example.tracker.ui.common.launchCatching
import java.time.LocalTime
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Settings screen: the app theme, the daily reminder and its time, and deleting all local data.
 * State is exposed as [uiState]; messages are sent once through [events].
 */
class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val isDeletingData = MutableStateFlow(false)

    private val _events = Channel<SettingsEvent>(Channel.BUFFERED)
    val events: Flow<SettingsEvent> = _events.receiveAsFlow()

    val uiState: StateFlow<SettingsUiState> =
        combine(
            settingsRepository.themeMode,
            isDeletingData,
            settingsRepository.nutritionReminderEnabled,
            settingsRepository.nutritionReminderTime,
        ) { themeMode, deleting, reminder, reminderTime ->
            SettingsUiState(
                themeMode = themeMode,
                isDeletingData = deleting,
                nutritionReminderEnabled = reminder,
                nutritionReminderTime = reminderTime,
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            SettingsUiState(
                themeMode = settingsRepository.themeMode.value,
                nutritionReminderEnabled = settingsRepository.nutritionReminderEnabled.value,
                nutritionReminderTime = settingsRepository.nutritionReminderTime.value,
            ),
        )

    fun onSelectThemeMode(mode: ThemeMode) {
        viewModelScope.launchCatching(onError = { showMessage(R.string.settings_error_theme) }) {
            settingsRepository.setThemeMode(mode)
        }
    }

    fun onSetNutritionReminder(enabled: Boolean) {
        viewModelScope.launchCatching(onError = { showMessage(R.string.settings_error_reminder) }) {
            settingsRepository.setNutritionReminderEnabled(enabled)
        }
    }

    fun onSetNutritionReminderTime(time: LocalTime) {
        viewModelScope.launchCatching(onError = { showMessage(R.string.settings_error_reminder) }) {
            settingsRepository.setNutritionReminderTime(time)
        }
    }

    fun onDeleteAllData() {
        if (isDeletingData.value) return
        isDeletingData.value = true
        viewModelScope.launchCatching(onError = { showMessage(R.string.settings_error_delete_data) }) {
            try {
                settingsRepository.deleteAllData()
                showMessage(R.string.settings_data_deleted)
            } finally {
                isDeletingData.value = false
            }
        }
    }

    private suspend fun showMessage(@StringRes messageRes: Int) {
        _events.send(SettingsEvent.ShowMessage(messageRes))
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as TrackerApplication).container
                SettingsViewModel(settingsRepository = container.settingsRepository)
            }
        }
    }
}
