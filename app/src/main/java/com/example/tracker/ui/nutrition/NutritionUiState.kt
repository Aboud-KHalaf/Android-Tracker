package com.example.tracker.ui.nutrition

import com.example.tracker.domain.model.DailyNutrition
import com.example.tracker.domain.model.DatePeriod
import com.example.tracker.domain.model.NutritionTargets
import com.example.tracker.domain.nutrition.NutritionSummary
import com.example.tracker.ui.nutrition.chart.BarChartModel
import java.time.LocalDate
import java.time.YearMonth

/** Everything the Nutrition screen renders. */
sealed interface NutritionUiState {
    data object Loading : NutritionUiState
    data object Error : NutritionUiState

    data class Success(
        val today: LocalDate,
        /** What was logged today, or null when nothing is yet. */
        val todayEntry: DailyNutrition?,
        val targets: NutritionTargets,
        val period: DatePeriod,
        val rangeStart: LocalDate,
        val rangeEnd: LocalDate,
        val metric: NutritionMetric,
        /** The selected metric per logged day; null when nothing is logged in the range. */
        val chart: BarChartModel?,
        /** Null when nothing is logged in the range. */
        val summary: NutritionSummary?,
        /** Logged days in the range, newest month first; empty when there are none. */
        val months: List<NutritionMonthUi>,
    ) : NutritionUiState {
        /** Calendar days in the range, logged or not. */
        val rangeDayCount: Int get() = (rangeEnd.toEpochDay() - rangeStart.toEpochDay() + 1).toInt()
    }
}

data class NutritionMonthUi(val month: YearMonth, val days: List<DailyNutrition>)
