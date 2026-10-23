package com.example.tracker.ui.stats

import androidx.annotation.StringRes

/** One-off effects the Stats tab handles once. */
sealed interface StatsEvent {
    data class ShowMessage(@StringRes val messageRes: Int) : StatsEvent
}
