package com.example.tracker.ui.nutrition

import com.example.tracker.domain.model.DailyNutrition
import com.example.tracker.domain.model.NutritionTargets
import com.example.tracker.domain.nutrition.NutritionPeriod
import com.example.tracker.domain.nutrition.NutritionSummary
import java.time.LocalDate
import java.time.YearMonth

/** Maps logged days and targets to [NutritionUiState.Success]. Pure. */
internal class NutritionStateMapper {

    /** [days] are the logged days from the period's start to its end, newest first. */
    fun map(
        today: LocalDate,
        todayEntry: DailyNutrition?,
        period: NutritionPeriod,
        days: List<DailyNutrition>,
        targets: NutritionTargets,
    ): NutritionUiState.Success = NutritionUiState.Success(
        today = today,
        todayEntry = todayEntry,
        targets = targets,
        period = period,
        rangeStart = period.start(today),
        rangeEnd = period.end(today),
        summary = NutritionSummary.of(days, targets),
        months = days
            .groupBy { YearMonth.from(it.date) }
            .map { (month, items) -> NutritionMonthUi(month, items) },
    )
}
