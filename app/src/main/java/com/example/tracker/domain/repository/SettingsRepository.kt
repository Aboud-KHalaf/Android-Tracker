package com.example.tracker.domain.repository

import com.example.tracker.domain.model.ThemeMode
import kotlinx.coroutines.flow.StateFlow

/** App preferences and whole-app data management. */
interface SettingsRepository {
    val themeMode: StateFlow<ThemeMode>

    suspend fun setThemeMode(mode: ThemeMode)

    /** Permanently deletes every exercise, plan and workout on this device. */
    suspend fun deleteAllData()
}
