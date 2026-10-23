package com.example.tracker.ui.history

import com.example.tracker.ui.common.WorkoutSummaryUi
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/** Everything the History screen renders. */
sealed interface HistoryUiState {
    data object Loading : HistoryUiState
    data object Error : HistoryUiState

    data class Success(
        /** "All" first, then one per plan. */
        val filters: List<PlanFilterUi>,
        /** The selected plan, or null for all workouts. */
        val selectedPlanId: String?,
        /** Newest month first; empty when nothing matches the filter. */
        val months: List<MonthGroupUi>,
        /** Every workout's streak and calendar, whatever the filter. */
        val consistency: ConsistencyUi = ConsistencyUi(weekStreak = 0, weeks = emptyList()),
    ) : HistoryUiState

}

/** A filter chip. [planId] null is "All"; [dayOfWeek] tells days with the same name apart. */
data class PlanFilterUi(val planId: String?, val name: String?, val dayOfWeek: DayOfWeek? = null)

data class MonthGroupUi(val month: YearMonth, val workouts: List<WorkoutSummaryUi>)

/**
 * The training calendar: [weeks] are columns, oldest first, each Monday to Sunday.
 * [weekStreak] counts weeks in a row with a workout.
 */
data class ConsistencyUi(val weekStreak: Int, val weeks: List<List<CalendarDayUi>>) {
    val trainedDayCount: Int get() = weeks.sumOf { week -> week.count { it.workoutCount > 0 } }
}

/** A day of the training calendar; [isFuture] days (later this week) are left blank. */
data class CalendarDayUi(val date: LocalDate, val workoutCount: Int, val isToday: Boolean, val isFuture: Boolean)
