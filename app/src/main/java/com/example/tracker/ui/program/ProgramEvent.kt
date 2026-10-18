package com.example.tracker.ui.program

import androidx.annotation.StringRes

/** One-off effects the Program screen handles once. */
sealed interface ProgramEvent {
    /** The program was deleted; leave the screen. */
    data object ProgramDeleted : ProgramEvent

    data class ShowMessage(@StringRes val messageRes: Int) : ProgramEvent
}
