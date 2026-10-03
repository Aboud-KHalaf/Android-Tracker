package com.example.tracker.ui.settings

import com.example.tracker.domain.model.ThemeMode

/** Settings are read synchronously, so the screen has no loading or error state. */
data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isDeletingData: Boolean = false,
)
