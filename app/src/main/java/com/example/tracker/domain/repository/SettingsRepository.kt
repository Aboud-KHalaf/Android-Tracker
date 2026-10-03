package com.example.tracker.domain.repository

import com.example.tracker.domain.model.NutritionTargets
import com.example.tracker.domain.model.ThemeMode
import kotlinx.coroutines.flow.StateFlow

/** App preferences and whole-app data management. */
interface SettingsRepository {
    val themeMode: StateFlow<ThemeMode>

    suspend fun setThemeMode(mode: ThemeMode)

    val nutritionTargets: StateFlow<NutritionTargets>

    /** Throws [IllegalArgumentException] when a target is out of range. */
    suspend fun setNutritionTargets(targets: NutritionTargets)

    /** Whether to send a notification at 9 PM when the day's nutrition isn't logged. On by default. */
    val nutritionReminderEnabled: StateFlow<Boolean>

    suspend fun setNutritionReminderEnabled(enabled: Boolean)

    /** Whether the app has already asked for notification permission on its own (asked once). */
    val notificationPermissionRequested: StateFlow<Boolean>

    suspend fun markNotificationPermissionRequested()

    /** Permanently deletes every exercise, plan, workout, nutrition and weight entry on this device. Preferences stay. */
    suspend fun deleteAllData()
}
