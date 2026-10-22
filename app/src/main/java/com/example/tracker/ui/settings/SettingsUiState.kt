package com.example.tracker.ui.settings

import com.example.tracker.domain.health.HealthAvailability
import com.example.tracker.domain.model.ThemeMode
import com.example.tracker.domain.reminder.ReminderTime
import com.example.tracker.domain.workout.RestTimes
import java.time.Instant
import java.time.LocalTime

/** Settings are read synchronously, so the screen has no loading or error state. */
data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val isDeletingData: Boolean = false,
    val isExportingData: Boolean = false,
    val isBackingUp: Boolean = false,
    val isRestoring: Boolean = false,
    val reminderEnabled: Boolean = true,
    val reminderTime: LocalTime = ReminderTime.DEFAULT,
    val restTimes: RestTimes = RestTimes(),
    val health: HealthUi = HealthUi(),
) {
    /** Only one of exporting, backing up, restoring or deleting runs at a time. */
    val isDataBusy: Boolean get() = isDeletingData || isExportingData || isBackingUp || isRestoring
}

/** The Health Connect section; hidden when this phone can't have Health Connect. */
data class HealthUi(
    val availability: HealthAvailability = HealthAvailability.UNSUPPORTED,
    val isEnabled: Boolean = false,
    val lastSync: Instant? = null,
    val isSyncing: Boolean = false,
)
