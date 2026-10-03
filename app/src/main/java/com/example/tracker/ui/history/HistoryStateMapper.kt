package com.example.tracker.ui.history

import com.example.tracker.domain.model.WorkoutPlan
import com.example.tracker.domain.model.WorkoutSummary
import com.example.tracker.ui.common.toWorkoutSummaryUi
import java.time.YearMonth
import java.time.ZoneId

/** Maps plans and finished workouts to [HistoryUiState.Success]. Pure. */
internal class HistoryStateMapper(private val zone: ZoneId) {

    /** [workouts] are newest first, already filtered to [selectedPlanId]. */
    fun map(plans: List<WorkoutPlan>, selectedPlanId: String?, workouts: List<WorkoutSummary>): HistoryUiState.Success =
        HistoryUiState.Success(
            filters = listOf(PlanFilterUi(planId = null, name = null)) + plans.map { PlanFilterUi(it.id, it.name) },
            selectedPlanId = selectedPlanId,
            months = workouts
                .map { it.toWorkoutSummaryUi(zone) }
                .groupBy { YearMonth.from(it.date) }
                .map { (month, items) -> MonthGroupUi(month, items) },
        )
}
