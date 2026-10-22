package com.example.tracker.domain.repository

import com.example.tracker.domain.model.ThemeMode
import com.example.tracker.domain.workout.RestTimes
import java.time.Instant
import java.time.LocalTime
import kotlinx.coroutines.flow.StateFlow

/** App preferences and whole-app data management. */
interface SettingsRepository {
    val themeMode: StateFlow<ThemeMode>

    suspend fun setThemeMode(mode: ThemeMode)

    /** Whether colors come from the wallpaper (Android 12+) instead of the blue brand theme. Off by default. */
    val dynamicColor: StateFlow<Boolean>

    suspend fun setDynamicColor(enabled: Boolean)

    /** Whether to send a notification at [reminderTime] when a stat isn't logged today. On by default. */
    val reminderEnabled: StateFlow<Boolean>

    suspend fun setReminderEnabled(enabled: Boolean)

    /** Local time of day for the daily reminder; 9 PM by default. */
    val reminderTime: StateFlow<LocalTime>

    suspend fun setReminderTime(time: LocalTime)

    /** How long to rest between sets and between exercises during a workout. */
    val restTimes: StateFlow<RestTimes>

    suspend fun setRestTimes(times: RestTimes)

    /** Whether steps, weight and sleep are read from Health Connect. Off by default. */
    val healthSyncEnabled: StateFlow<Boolean>

    suspend fun setHealthSyncEnabled(enabled: Boolean)

    /** When Health Connect was last read, or null if never. */
    val lastHealthSync: StateFlow<Instant?>

    suspend fun setLastHealthSync(at: Instant)

    /** Whether onboarding was finished or skipped (or wasn't needed); see FirstLaunch. */
    val onboardingDone: StateFlow<Boolean>

    suspend fun setOnboardingDone()

    /** Whether the app has already asked for notification permission on its own (asked once). */
    val notificationPermissionRequested: StateFlow<Boolean>

    suspend fun markNotificationPermissionRequested()

    /** Permanently deletes every exercise, plan, workout, stat and progress photo on this device. Preferences stay. */
    suspend fun deleteAllData()
}
