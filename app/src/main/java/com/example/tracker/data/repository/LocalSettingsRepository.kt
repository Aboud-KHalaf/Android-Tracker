package com.example.tracker.data.repository

import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.tracker.data.local.TrackerDatabase
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
 * The theme is read once at startup so the first frame already uses it.
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

    override suspend fun deleteAllData() {
        withContext(ioDispatcher) { database.clearAllTables() }
    }

    private fun readThemeMode(): ThemeMode {
        val stored = preferences.getString(KEY_THEME_MODE, null)
        return ThemeMode.entries.firstOrNull { it.name == stored } ?: ThemeMode.SYSTEM
    }

    companion object {
        const val PREFERENCES_NAME = "settings"
        private const val KEY_THEME_MODE = "theme_mode"
    }
}
