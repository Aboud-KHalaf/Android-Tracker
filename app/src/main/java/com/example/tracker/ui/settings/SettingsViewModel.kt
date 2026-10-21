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
import com.example.tracker.data.health.HealthConnectDataSource
import com.example.tracker.domain.export.ExportFormat
import com.example.tracker.domain.health.HealthAvailability
import com.example.tracker.domain.export.ReportExporter
import com.example.tracker.domain.export.exportFileName
import com.example.tracker.domain.model.ThemeMode
import com.example.tracker.domain.repository.CustomStatRepository
import com.example.tracker.domain.repository.ExerciseRepository
import com.example.tracker.domain.repository.SettingsRepository
import com.example.tracker.domain.repository.WorkoutRepository
import com.example.tracker.domain.workout.RestTimes
import com.example.tracker.ui.common.launchCatching
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.CancellationException
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
 * Settings screen: the app theme, the daily reminder and its time, workout rest times, and
 * exporting or deleting all local data. State is exposed as [uiState]; messages are sent once through [events].
 */
class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val data: ExportableData,
    private val reportExporter: ReportExporter,
    private val time: TimeProvider,
    private val health: Health = Health.None,
) : ViewModel() {

    /** Health Connect: whether it's there, the permissions to ask for, and the sync itself. */
    data class Health(
        val availability: () -> HealthAvailability,
        val permissions: Set<String>,
        val sync: suspend () -> Int,
    ) {
        companion object {
            val None = Health(availability = { HealthAvailability.UNSUPPORTED }, permissions = emptySet(), sync = { 0 })
        }
    }

    /** The read permissions to ask Health Connect for when sync is turned on. */
    val healthPermissions: Set<String> get() = health.permissions

    /** The repositories "Export all data" reads from. */
    data class ExportableData(
        val exercises: ExerciseRepository,
        val workouts: WorkoutRepository,
        val stats: CustomStatRepository,
    )

    private val isDeletingData = MutableStateFlow(false)
    private val isExportingData = MutableStateFlow(false)
    private val healthAvailability = MutableStateFlow(health.availability())
    private val isSyncingHealth = MutableStateFlow(false)

    private val healthUi = combine(
        healthAvailability,
        settingsRepository.healthSyncEnabled,
        settingsRepository.lastHealthSync,
        isSyncingHealth,
    ) { availability, enabled, lastSync, syncing -> HealthUi(availability, enabled, lastSync, syncing) }

    private val _events = Channel<SettingsEvent>(Channel.BUFFERED)
    val events: Flow<SettingsEvent> = _events.receiveAsFlow()

    val uiState: StateFlow<SettingsUiState> =
        combine(
            settingsRepository.themeMode,
            combine(isDeletingData, isExportingData, healthUi, ::Triple),
            settingsRepository.reminderEnabled,
            settingsRepository.reminderTime,
            settingsRepository.restTimes,
        ) { themeMode, (deleting, exporting, health), reminder, reminderTime, restTimes ->
            SettingsUiState(
                themeMode = themeMode,
                isDeletingData = deleting,
                isExportingData = exporting,
                reminderEnabled = reminder,
                reminderTime = reminderTime,
                restTimes = restTimes,
                health = health,
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            SettingsUiState(
                themeMode = settingsRepository.themeMode.value,
                reminderEnabled = settingsRepository.reminderEnabled.value,
                reminderTime = settingsRepository.reminderTime.value,
                restTimes = settingsRepository.restTimes.value,
                health = HealthUi(
                    availability = healthAvailability.value,
                    isEnabled = settingsRepository.healthSyncEnabled.value,
                    lastSync = settingsRepository.lastHealthSync.value,
                ),
            ),
        )

    fun onSelectThemeMode(mode: ThemeMode) {
        viewModelScope.launchCatching(onError = { showMessage(R.string.settings_error_theme) }) {
            settingsRepository.setThemeMode(mode)
        }
    }

    fun onSetReminder(enabled: Boolean) {
        viewModelScope.launchCatching(onError = { showMessage(R.string.settings_error_reminder) }) {
            settingsRepository.setReminderEnabled(enabled)
        }
    }

    fun onSetReminderTime(time: LocalTime) {
        viewModelScope.launchCatching(onError = { showMessage(R.string.settings_error_reminder) }) {
            settingsRepository.setReminderTime(time)
        }
    }

    fun onSetRestBetweenSets(seconds: Int) = updateRestTimes { it.copy(betweenSetsSeconds = seconds) }

    fun onSetRestBetweenExercises(seconds: Int) = updateRestTimes { it.copy(betweenExercisesSeconds = seconds) }

    private fun updateRestTimes(change: (RestTimes) -> RestTimes) {
        viewModelScope.launchCatching(onError = { showMessage(R.string.settings_error_rest) }) {
            settingsRepository.setRestTimes(change(settingsRepository.restTimes.value))
        }
    }

    /** Back on screen: Health Connect may have been installed meanwhile. */
    fun onResume() {
        healthAvailability.value = health.availability()
    }

    /**
     * Health Connect answered the request for [healthPermissions] with [granted]. Any of them
     * turns sync on and syncs at once; none leaves it off.
     */
    fun onHealthPermissionsResult(granted: Set<String>) {
        if (granted.none { it in health.permissions }) {
            viewModelScope.launchCatching(onError = {}) { showMessage(R.string.settings_health_denied) }
            return
        }
        viewModelScope.launchCatching(onError = { showMessage(R.string.settings_error_health_sync) }) {
            settingsRepository.setHealthSyncEnabled(true)
            syncHealth()
        }
    }

    fun onDisableHealthSync() {
        viewModelScope.launchCatching(onError = { showMessage(R.string.settings_error_health_sync) }) {
            settingsRepository.setHealthSyncEnabled(false)
        }
    }

    fun onSyncHealthNow() {
        viewModelScope.launchCatching(onError = {}) { syncHealth() }
    }

    private suspend fun syncHealth() {
        if (isSyncingHealth.value) return
        isSyncingHealth.value = true
        try {
            health.sync()
            showMessage(R.string.settings_health_synced)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            showMessage(R.string.settings_error_health_sync)
        } finally {
            isSyncingHealth.value = false
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

    /** Saves every exercise, workout set and stat entry as [format] to [destination], a file URI. */
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
                    stats = data.stats.observeOverviews().first().map { overview ->
                        overview.stat to data.stats.observeEntries(overview.stat.id, HISTORY_START, today).first()
                    },
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
                        stats = container.customStatRepository,
                    ),
                    reportExporter = container.reportExporter,
                    time = container.time,
                    health = Health(
                        availability = container.healthDataSource::availability,
                        permissions = HealthConnectDataSource.PERMISSIONS.values.toSet(),
                        sync = container.healthSync::sync,
                    ),
                )
            }
        }
    }
}
