package com.example.tracker.domain.repository

import com.example.tracker.domain.model.ThemeMode
import java.time.LocalTime
import kotlinx.coroutines.flow.StateFlow

/** App preferences and whole-app data management. */
interface SettingsRepository {
    val themeMode: StateFlow<ThemeMode>

    suspend fun setThemeMode(mode: ThemeMode)

    /** Whether to send a notification at [reminderTime] when a stat isn't logged today. On by default. */
    val reminderEnabled: StateFlow<Boolean>

    suspend fun setReminderEnabled(enabled: Boolean)

    /** Local time of day for the daily reminder; 9 PM by default. */
    val reminderTime: StateFlow<LocalTime>

    suspend fun setReminderTime(time: LocalTime)

    /** Whether the app has already asked for notification permission on its own (asked once). */
    val notificationPermissionRequested: StateFlow<Boolean>

    suspend fun markNotificationPermissionRequested()

    /** Permanently deletes every exercise, plan, workout, stat and progress photo on this device. Preferences stay. */
    suspend fun deleteAllData()
}
