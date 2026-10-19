package com.example.tracker.ui.stats.detail

import com.example.tracker.domain.model.DatePeriod
import com.example.tracker.domain.model.StatEntry
import com.example.tracker.domain.model.StatOverview
import com.example.tracker.domain.stats.StatSummary
import com.example.tracker.ui.common.chart.ChartModel
import com.example.tracker.ui.stats.toCardUi
import java.time.LocalDate
import java.time.YearMonth

/** Maps a stat and its entries to [StatDetailUiState.Success]. Pure. */
internal class StatDetailStateMapper {

    /** [entries] are the stat's entries from the period's start to its end, newest first. */
    fun map(today: LocalDate, overview: StatOverview, period: DatePeriod, entries: List<StatEntry>): StatDetailUiState.Success {
        val oldestFirst = entries.sortedBy { it.date }
        val rows = oldestFirst.mapIndexed { i, entry ->
            StatRowUi(entry, change = oldestFirst.getOrNull(i - 1)?.let { entry.value - it.value })
        }.asReversed()
        return StatDetailUiState.Success(
            today = today,
            stat = overview.stat,
            latest = overview.latest,
            progress = overview.toCardUi().progress,
            period = period,
            rangeStart = period.start(today),
            rangeEnd = period.end(today),
            summary = StatSummary.of(entries),
            chart = ChartModel.build(oldestFirst.map { it.date to it.value }),
            months = rows
                .groupBy { YearMonth.from(it.entry.date) }
                .map { (month, items) -> StatMonthUi(month, items) },
        )
    }
}
