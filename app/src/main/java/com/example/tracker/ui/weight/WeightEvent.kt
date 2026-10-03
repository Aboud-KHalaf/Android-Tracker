package com.example.tracker.ui.weight

import androidx.annotation.StringRes
import com.example.tracker.domain.model.WeightEntry

/** One-off effects the Weight screen handles once. */
sealed interface WeightEvent {
    data class ShowMessage(@StringRes val messageRes: Int) : WeightEvent

    /** [entry] was deleted; offer to undo. */
    data class EntryDeleted(val entry: WeightEntry) : WeightEvent
}
