package com.example.tracker.ui.stats.detail

import androidx.annotation.StringRes
import com.example.tracker.domain.model.CustomStat
import com.example.tracker.domain.model.DatePeriod
import com.example.tracker.domain.model.StatEntry
import com.example.tracker.domain.stats.StatSummary
import com.example.tracker.ui.common.chart.ChartModel
import java.time.LocalDate
import java.time.YearMonth

/** Everything a custom stat's screen renders. */
sealed interface StatDetailUiState {
    data object Loading : StatDetailUiState
    data object Error : StatDetailUiState

    /** The stat was deleted, e.g. from its editor; the screen closes. */
    data object Missing : StatDetailUiState

    data class Success(
        val today: LocalDate,
        val stat: CustomStat,
        /** The most recent entry ever, or null when nothing is logged yet. */
        val latest: StatEntry?,
        /** From 0 to 1, from the first entry ever toward the target; null without either. */
        val progress: Float?,
        val period: DatePeriod,
        val rangeStart: LocalDate,
        val rangeEnd: LocalDate,
        /** Null when nothing is logged in the range. */
        val summary: StatSummary?,
        /** Values over the range, oldest first; null when nothing is logged in it. */
        val chart: ChartModel?,
        /** Entries in the range, newest month first. */
        val months: List<StatMonthUi>,
    ) : StatDetailUiState
}

data class StatMonthUi(val month: YearMonth, val entries: List<StatRowUi>)

/** An entry and its change from the entry before it, if that one is in the range. */
data class StatRowUi(val entry: StatEntry, val change: Double?)

/** The open "Log" dialog. The value is kept as typed so the user can edit it freely. */
data class StatEntryEditorState(
    val date: LocalDate,
    val value: String,
    /** The stat's decimal places, which the value is typed with. */
    val decimals: Int,
    /** Whether [date] already has an entry: saving replaces it, and it can be deleted. */
    val isExisting: Boolean,
    val valueError: Boolean = false,
    val isSaving: Boolean = false,
)

/** One-off effects the stat screen handles once. */
sealed interface StatDetailEvent {
    data class ShowMessage(@StringRes val messageRes: Int) : StatDetailEvent

    /** [entry] was deleted; offer to undo. */
    data class EntryDeleted(val entry: StatEntry) : StatDetailEvent
}
