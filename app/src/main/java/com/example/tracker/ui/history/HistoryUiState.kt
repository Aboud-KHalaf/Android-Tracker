package com.example.tracker.ui.history

import com.example.tracker.ui.common.WorkoutSummaryUi
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
    ) : HistoryUiState

}

/** A filter chip. [planId] null is "All". */
data class PlanFilterUi(val planId: String?, val name: String?)

data class MonthGroupUi(val month: YearMonth, val workouts: List<WorkoutSummaryUi>)
