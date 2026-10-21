package com.example.tracker.data.repository

import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.tracker.data.local.ProgressPhotoStore
import com.example.tracker.data.local.TrackerDatabase
import com.example.tracker.domain.model.ThemeMode
import com.example.tracker.domain.reminder.ReminderTime
import com.example.tracker.domain.repository.SettingsRepository
import com.example.tracker.domain.workout.RestTimes
import java.time.Instant
import java.time.LocalTime
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Keeps preferences in [SharedPreferences] and deletes data from the local database and
 * the progress photo files.
 * Preferences are read once at startup, so the first frame already uses the saved theme.
 */
class LocalSettingsRepository(
    private val preferences: SharedPreferences,
    private val database: TrackerDatabase,
    private val photoStore: ProgressPhotoStore,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : SettingsRepository {

    private val _themeMode = MutableStateFlow(readThemeMode())
    override val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    override suspend fun setThemeMode(mode: ThemeMode) {
        withContext(ioDispatcher) {
            preferences.edit(commit = true) { putString(KEY_THEME_MODE, mode.name) }
        }
        _themeMode.value = mode
    }

    private val _reminderEnabled = MutableStateFlow(preferences.getBoolean(KEY_REMINDER, true))
    override val reminderEnabled: StateFlow<Boolean> = _reminderEnabled.asStateFlow()

    override suspend fun setReminderEnabled(enabled: Boolean) {
        withContext(ioDispatcher) {
            preferences.edit(commit = true) { putBoolean(KEY_REMINDER, enabled) }
        }
        _reminderEnabled.value = enabled
    }

    private val _reminderTime = MutableStateFlow(readReminderTime())
    override val reminderTime: StateFlow<LocalTime> = _reminderTime.asStateFlow()

    override suspend fun setReminderTime(time: LocalTime) {
        val minuteOfDay = time.hour * MINUTES_PER_HOUR + time.minute
        withContext(ioDispatcher) {
            preferences.edit(commit = true) { putInt(KEY_REMINDER_MINUTE, minuteOfDay) }
        }
        _reminderTime.value = LocalTime.of(time.hour, time.minute)
    }

    private val _restTimes = MutableStateFlow(readRestTimes())
    override val restTimes: StateFlow<RestTimes> = _restTimes.asStateFlow()

    override suspend fun setRestTimes(times: RestTimes) {
        withContext(ioDispatcher) {
            preferences.edit(commit = true) {
                putInt(KEY_REST_BETWEEN_SETS, times.betweenSetsSeconds)
                putInt(KEY_REST_BETWEEN_EXERCISES, times.betweenExercisesSeconds)
            }
        }
        _restTimes.value = times
    }

    private val _healthSyncEnabled = MutableStateFlow(preferences.getBoolean(KEY_HEALTH_SYNC, false))
    override val healthSyncEnabled: StateFlow<Boolean> = _healthSyncEnabled.asStateFlow()

    override suspend fun setHealthSyncEnabled(enabled: Boolean) {
        withContext(ioDispatcher) {
            preferences.edit(commit = true) { putBoolean(KEY_HEALTH_SYNC, enabled) }
        }
        _healthSyncEnabled.value = enabled
    }

    private val _lastHealthSync = MutableStateFlow(
        preferences.getLong(KEY_LAST_HEALTH_SYNC, 0L).takeIf { it > 0L }?.let(Instant::ofEpochMilli),
    )
    override val lastHealthSync: StateFlow<Instant?> = _lastHealthSync.asStateFlow()

    override suspend fun setLastHealthSync(at: Instant) {
        withContext(ioDispatcher) {
            preferences.edit(commit = true) { putLong(KEY_LAST_HEALTH_SYNC, at.toEpochMilli()) }
        }
        _lastHealthSync.value = at
    }

    private val _notificationPermissionRequested =
        MutableStateFlow(preferences.getBoolean(KEY_NOTIFICATION_PERMISSION_REQUESTED, false))
    override val notificationPermissionRequested: StateFlow<Boolean> = _notificationPermissionRequested.asStateFlow()

    override suspend fun markNotificationPermissionRequested() {
        withContext(ioDispatcher) {
            preferences.edit(commit = true) { putBoolean(KEY_NOTIFICATION_PERMISSION_REQUESTED, true) }
        }
        _notificationPermissionRequested.value = true
    }

    override suspend fun deleteAllData() {
        withContext(ioDispatcher) {
            database.clearAllTables()
            photoStore.deleteAll()
        }
    }

    private fun readThemeMode(): ThemeMode {
        val stored = preferences.getString(KEY_THEME_MODE, null)
        return ThemeMode.entries.firstOrNull { it.name == stored } ?: ThemeMode.SYSTEM
    }

    /** Stored as minutes after midnight; anything unreadable falls back to the default. */
    private fun readReminderTime(): LocalTime {
        val minuteOfDay = preferences.getIntOrNull(KEY_REMINDER_MINUTE)
            ?.takeIf { it in 0 until MINUTES_PER_DAY }
            ?: return ReminderTime.DEFAULT
        return LocalTime.of(minuteOfDay / MINUTES_PER_HOUR, minuteOfDay % MINUTES_PER_HOUR)
    }

    /** Anything unreadable (e.g. negative) falls back to the default. */
    private fun readRestTimes() = RestTimes(
        betweenSetsSeconds = preferences.getIntOrNull(KEY_REST_BETWEEN_SETS)?.takeIf { it >= 0 }
            ?: RestTimes.DEFAULT_BETWEEN_SETS_SECONDS,
        betweenExercisesSeconds = preferences.getIntOrNull(KEY_REST_BETWEEN_EXERCISES)?.takeIf { it >= 0 }
            ?: RestTimes.DEFAULT_BETWEEN_EXERCISES_SECONDS,
    )

    private fun SharedPreferences.getIntOrNull(key: String): Int? = if (contains(key)) getInt(key, 0) else null

    companion object {
        const val PREFERENCES_NAME = "settings"
        private const val KEY_THEME_MODE = "theme_mode"
        // Named for the daily log the reminder started with; kept so the saved choice carries over.
        private const val KEY_REMINDER = "nutrition_reminder"
        private const val KEY_REMINDER_MINUTE = "nutrition_reminder_minute"
        private const val MINUTES_PER_HOUR = 60
        private const val MINUTES_PER_DAY = 24 * MINUTES_PER_HOUR
        private const val KEY_REST_BETWEEN_SETS = "rest_between_sets_seconds"
        private const val KEY_REST_BETWEEN_EXERCISES = "rest_between_exercises_seconds"
        private const val KEY_NOTIFICATION_PERMISSION_REQUESTED = "notification_permission_requested"
        private const val KEY_HEALTH_SYNC = "health_connect_sync"
        private const val KEY_LAST_HEALTH_SYNC = "health_connect_last_sync"
    }
}
