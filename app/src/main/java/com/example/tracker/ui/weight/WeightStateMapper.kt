package com.example.tracker.ui.weight

import com.example.tracker.domain.model.DatePeriod
import com.example.tracker.domain.model.WeightEntry
import com.example.tracker.domain.weight.WeightSummary
import com.example.tracker.ui.common.chart.ChartModel
import java.time.LocalDate
import java.time.YearMonth

/** Maps weight entries to [WeightUiState.Success]. Pure. */
internal class WeightStateMapper {

    /** [entries] are the entries from the period's start to its end, newest first. */
    fun map(today: LocalDate, latest: WeightEntry?, period: DatePeriod, entries: List<WeightEntry>): WeightUiState.Success {
        val oldestFirst = entries.sortedBy { it.date }
        val rows = oldestFirst.mapIndexed { i, entry ->
            WeightRowUi(entry, changeKg = oldestFirst.getOrNull(i - 1)?.let { entry.weightKg - it.weightKg })
        }.asReversed()
        return WeightUiState.Success(
            today = today,
            latest = latest,
            period = period,
            rangeStart = period.start(today),
            rangeEnd = period.end(today),
            summary = WeightSummary.of(entries),
            chart = ChartModel.build(oldestFirst.map { it.date to it.weightKg }),
            months = rows
                .groupBy { YearMonth.from(it.entry.date) }
                .map { (month, items) -> WeightMonthUi(month, items) },
        )
    }
}
