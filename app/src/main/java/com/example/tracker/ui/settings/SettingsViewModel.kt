package com.example.tracker.ui.settings

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.tracker.R
import com.example.tracker.TrackerApplication
import com.example.tracker.core.TimeProvider
import com.example.tracker.domain.export.ExportFormat
import com.example.tracker.domain.export.ReportExporter
import com.example.tracker.domain.export.exportFileName
import com.example.tracker.domain.model.ThemeMode
import com.example.tracker.domain.repository.ExerciseRepository
import com.example.tracker.domain.repository.NutritionRepository
import com.example.tracker.domain.repository.SettingsRepository
import com.example.tracker.domain.repository.WeightRepository
import com.example.tracker.domain.repository.WorkoutRepository
import com.example.tracker.ui.common.launchCatching
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Settings screen: the app theme, the daily reminder and its time, and exporting or deleting all
 * local data. State is exposed as [uiState]; messages are sent once through [events].
 */
class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val data: ExportableData,
    private val reportExporter: ReportExporter,
    private val time: TimeProvider,
) : ViewModel() {

    /** The repositories "Export all data" reads from. */
    data class ExportableData(
        val exercises: ExerciseRepository,
        val workouts: WorkoutRepository,
        val nutrition: NutritionRepository,
        val weights: WeightRepository,
    )

    private val isDeletingData = MutableStateFlow(false)
    private val isExportingData = MutableStateFlow(false)

    private val _events = Channel<SettingsEvent>(Channel.BUFFERED)
    val events: Flow<SettingsEvent> = _events.receiveAsFlow()

    val uiState: StateFlow<SettingsUiState> =
        combine(
            settingsRepository.themeMode,
            combine(isDeletingData, isExportingData, ::Pair),
            settingsRepository.nutritionReminderEnabled,
            settingsRepository.nutritionReminderTime,
        ) { themeMode, (deleting, exporting), reminder, reminderTime ->
            SettingsUiState(
                themeMode = themeMode,
                isDeletingData = deleting,
                isExportingData = exporting,
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

    fun exportFileName(format: ExportFormat): String = exportFileName("all-data", today(), format)

    /** Saves every exercise, workout set, logged day and weight entry as [format] to [destination], a file URI. */
    fun onExportAllData(format: ExportFormat, destination: String) {
        if (isExportingData.value) return
        isExportingData.value = true
        viewModelScope.launchCatching(onError = { showMessage(R.string.export_error) }) {
            try {
                val today = today()
                val report = FullExportReportBuilder(time.zone()).build(
                    exportedOn = today,
                    exercises = data.exercises.observeExercises().first(),
                    sessions = data.workouts.observeAllSessions().first(),
                    days = data.nutrition.observeDays(HISTORY_START, today).first(),
                    targets = settingsRepository.nutritionTargets.value,
                    weights = data.weights.observeEntries(HISTORY_START, today).first(),
                )
                reportExporter.export(report, format, destination)
                showMessage(R.string.export_done)
            } finally {
                isExportingData.value = false
            }
        }
    }

    private fun today(): LocalDate = time.now().atZone(time.zone()).toLocalDate()

    private suspend fun showMessage(@StringRes messageRes: Int) {
        _events.send(SettingsEvent.ShowMessage(messageRes))
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        /** Before anything could have been logged, so "from here to today" is all history. */
        private val HISTORY_START: LocalDate = LocalDate.of(1900, 1, 1)

        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as TrackerApplication).container
                SettingsViewModel(
                    settingsRepository = container.settingsRepository,
                    data = ExportableData(
                        exercises = container.exerciseRepository,
                        workouts = container.workoutRepository,
                        nutrition = container.nutritionRepository,
                        weights = container.weightRepository,
                    ),
                    reportExporter = container.reportExporter,
                    time = container.time,
                )
            }
        }
    }
}
