package com.example.tracker.data.repository

import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.tracker.data.local.TrackerDatabase
import com.example.tracker.domain.model.DailyNutrition
import com.example.tracker.domain.model.NutritionTargets
import com.example.tracker.domain.model.ThemeMode
import com.example.tracker.domain.reminder.ReminderTime
import com.example.tracker.domain.repository.SettingsRepository
import java.time.LocalTime
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Keeps preferences in [SharedPreferences] and deletes data from the local database.
 * Preferences are read once at startup, so the first frame already uses the saved theme.
 */
class LocalSettingsRepository(
    private val preferences: SharedPreferences,
    private val database: TrackerDatabase,
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

    private val _nutritionTargets = MutableStateFlow(readNutritionTargets())
    override val nutritionTargets: StateFlow<NutritionTargets> = _nutritionTargets.asStateFlow()

    override suspend fun setNutritionTargets(targets: NutritionTargets) {
        targets.calories?.let { require(it in 1..DailyNutrition.MAX_CALORIES) { "Calorie target out of range: $it" } }
        targets.proteinGrams?.let { require(it in 1..DailyNutrition.MAX_PROTEIN_GRAMS) { "Protein target out of range: $it" } }
        withContext(ioDispatcher) {
            preferences.edit(commit = true) {
                putOrRemove(KEY_CALORIE_TARGET, targets.calories)
                putOrRemove(KEY_PROTEIN_TARGET, targets.proteinGrams)
            }
        }
        _nutritionTargets.value = targets
    }

    private val _nutritionReminderEnabled = MutableStateFlow(preferences.getBoolean(KEY_NUTRITION_REMINDER, true))
    override val nutritionReminderEnabled: StateFlow<Boolean> = _nutritionReminderEnabled.asStateFlow()

    override suspend fun setNutritionReminderEnabled(enabled: Boolean) {
        withContext(ioDispatcher) {
            preferences.edit(commit = true) { putBoolean(KEY_NUTRITION_REMINDER, enabled) }
        }
        _nutritionReminderEnabled.value = enabled
    }

    private val _nutritionReminderTime = MutableStateFlow(readNutritionReminderTime())
    override val nutritionReminderTime: StateFlow<LocalTime> = _nutritionReminderTime.asStateFlow()

    override suspend fun setNutritionReminderTime(time: LocalTime) {
        val minuteOfDay = time.hour * MINUTES_PER_HOUR + time.minute
        withContext(ioDispatcher) {
            preferences.edit(commit = true) { putInt(KEY_NUTRITION_REMINDER_MINUTE, minuteOfDay) }
        }
        _nutritionReminderTime.value = LocalTime.of(time.hour, time.minute)
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
        withContext(ioDispatcher) { database.clearAllTables() }
    }

    private fun readThemeMode(): ThemeMode {
        val stored = preferences.getString(KEY_THEME_MODE, null)
        return ThemeMode.entries.firstOrNull { it.name == stored } ?: ThemeMode.SYSTEM
    }

    private fun readNutritionTargets() = NutritionTargets(
        calories = preferences.getIntOrNull(KEY_CALORIE_TARGET),
        proteinGrams = preferences.getIntOrNull(KEY_PROTEIN_TARGET),
    )

    /** Stored as minutes after midnight; anything unreadable falls back to the default. */
    private fun readNutritionReminderTime(): LocalTime {
        val minuteOfDay = preferences.getIntOrNull(KEY_NUTRITION_REMINDER_MINUTE)
            ?.takeIf { it in 0 until MINUTES_PER_DAY }
            ?: return ReminderTime.DEFAULT
        return LocalTime.of(minuteOfDay / MINUTES_PER_HOUR, minuteOfDay % MINUTES_PER_HOUR)
    }

    private fun SharedPreferences.getIntOrNull(key: String): Int? = if (contains(key)) getInt(key, 0) else null

    private fun SharedPreferences.Editor.putOrRemove(key: String, value: Int?) {
        if (value == null) remove(key) else putInt(key, value)
    }

    companion object {
        const val PREFERENCES_NAME = "settings"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_CALORIE_TARGET = "calorie_target"
        private const val KEY_PROTEIN_TARGET = "protein_target"
        private const val KEY_NUTRITION_REMINDER = "nutrition_reminder"
        private const val KEY_NUTRITION_REMINDER_MINUTE = "nutrition_reminder_minute"
        private const val MINUTES_PER_HOUR = 60
        private const val MINUTES_PER_DAY = 24 * MINUTES_PER_HOUR
        private const val KEY_NOTIFICATION_PERMISSION_REQUESTED = "notification_permission_requested"
    }
}
