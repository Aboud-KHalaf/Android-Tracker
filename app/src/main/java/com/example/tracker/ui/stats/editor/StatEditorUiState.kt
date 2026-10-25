package com.example.tracker.ui.stats.editor

import androidx.annotation.StringRes
import com.example.tracker.domain.model.StatColor
import com.example.tracker.domain.model.StatGoal
import com.example.tracker.domain.model.StatIcon

/** The stat as typed; [target] is kept as text so it can be edited freely, and empty means none. */
data class StatForm(
    val name: String = "",
    val unit: String = "",
    val decimals: Int = 1,
    val target: String = "",
    val icon: StatIcon = StatIcon.CHART,
    val color: StatColor = StatColor.BLUE,
    val goal: StatGoal = StatGoal.CHANGE,
)

/** Everything the stat editor renders. */
sealed interface StatEditorUiState {
    data object Loading : StatEditorUiState

    /** The stat couldn't be loaded, or no longer exists. */
    data object Error : StatEditorUiState

    data class Editing(
        val form: StatForm,
        /** Creating a new stat rather than editing one. */
        val isNew: Boolean,
        val nameError: Boolean = false,
        val targetError: Boolean = false,
        val isSaving: Boolean = false,
    ) : StatEditorUiState
}

/** One-off effects the stat editor handles once. */
sealed interface StatEditorEvent {
    data class ShowMessage(@StringRes val messageRes: Int) : StatEditorEvent

    data class Created(val statId: String) : StatEditorEvent

    data object Saved : StatEditorEvent

    data object Deleted : StatEditorEvent
}
