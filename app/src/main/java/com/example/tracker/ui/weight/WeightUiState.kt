package com.example.tracker.ui.weight

import com.example.tracker.domain.model.DatePeriod
import com.example.tracker.domain.model.WeightEntry
import com.example.tracker.domain.weight.WeightSummary
import com.example.tracker.ui.common.chart.ChartModel
import java.time.LocalDate
import java.time.YearMonth

/** Everything the Weight screen renders. */
sealed interface WeightUiState {
    data object Loading : WeightUiState
    data object Error : WeightUiState

    data class Success(
        val today: LocalDate,
        /** The most recent entry ever, or null when nothing is logged yet. */
        val latest: WeightEntry?,
        val period: DatePeriod,
        val rangeStart: LocalDate,
        val rangeEnd: LocalDate,
        /** Null when nothing is logged in the range. */
        val summary: WeightSummary?,
        /** Weight over the range, oldest first; null when nothing is logged in it. */
        val chart: ChartModel?,
        /** Entries in the range, newest month first. */
        val months: List<WeightMonthUi>,
    ) : WeightUiState
}

data class WeightMonthUi(val month: YearMonth, val entries: List<WeightRowUi>)

/** An entry and its change from the entry before it, if that one is in the range. */
data class WeightRowUi(val entry: WeightEntry, val changeKg: Double?)
