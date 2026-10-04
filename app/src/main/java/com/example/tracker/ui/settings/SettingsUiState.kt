package com.example.tracker.ui.settings

import com.example.tracker.domain.model.ThemeMode
import com.example.tracker.domain.reminder.ReminderTime
import java.time.LocalTime

/** Settings are read synchronously, so the screen has no loading or error state. */
data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isDeletingData: Boolean = false,
    val nutritionReminderEnabled: Boolean = true,
    val nutritionReminderTime: LocalTime = ReminderTime.DEFAULT,
)
