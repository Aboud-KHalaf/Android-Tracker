package com.example.tracker.ui.nutrition

import com.example.tracker.domain.model.DailyNutrition
import com.example.tracker.domain.model.NutritionTargets
import com.example.tracker.domain.nutrition.NutritionPeriod
import com.example.tracker.domain.nutrition.NutritionSummary
import com.example.tracker.ui.nutrition.chart.BarChartModel
import java.time.LocalDate
import java.time.YearMonth

/** Maps logged days and targets to [NutritionUiState.Success]. Pure. */
internal class NutritionStateMapper {

    /** [days] are the logged days from the period's start to its end, newest first. */
    fun map(
        today: LocalDate,
        todayEntry: DailyNutrition?,
        period: NutritionPeriod,
        metric: NutritionMetric,
        days: List<DailyNutrition>,
        targets: NutritionTargets,
    ): NutritionUiState.Success {
        val start = period.start(today)
        val end = period.end(today)
        return NutritionUiState.Success(
            today = today,
            todayEntry = todayEntry,
            targets = targets,
            period = period,
            rangeStart = start,
            rangeEnd = end,
            metric = metric,
            chart = BarChartModel.build(start, end, days, metric, targets),
            summary = NutritionSummary.of(days, targets),
            months = days
                .groupBy { YearMonth.from(it.date) }
                .map { (month, items) -> NutritionMonthUi(month, items) },
        )
    }
}
