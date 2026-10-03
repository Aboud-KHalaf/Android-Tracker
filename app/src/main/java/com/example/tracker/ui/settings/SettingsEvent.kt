package com.example.tracker.ui.settings

import androidx.annotation.StringRes

/** One-off effects the Settings screen handles once. */
sealed interface SettingsEvent {
    data class ShowMessage(@StringRes val messageRes: Int) : SettingsEvent
}
