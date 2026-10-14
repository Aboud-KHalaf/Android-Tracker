package com.example.tracker.ui.photos

import androidx.annotation.StringRes

/** One-off effects the Progress photos screen handles once. */
sealed interface ProgressPhotosEvent {
    data class ShowMessage(@StringRes val messageRes: Int) : ProgressPhotosEvent
}
