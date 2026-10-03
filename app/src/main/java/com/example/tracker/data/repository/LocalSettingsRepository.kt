package com.example.tracker.data.repository

import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.tracker.data.local.TrackerDatabase
import com.example.tracker.domain.model.DailyNutrition
import com.example.tracker.domain.model.NutritionTargets
import com.example.tracker.domain.model.ThemeMode
import com.example.tracker.domain.repository.SettingsRepository
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

    private fun SharedPreferences.getIntOrNull(key: String): Int? = if (contains(key)) getInt(key, 0) else null

    private fun SharedPreferences.Editor.putOrRemove(key: String, value: Int?) {
        if (value == null) remove(key) else putInt(key, value)
    }

    companion object {
        const val PREFERENCES_NAME = "settings"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_CALORIE_TARGET = "calorie_target"
        private const val KEY_PROTEIN_TARGET = "protein_target"
    }
}
