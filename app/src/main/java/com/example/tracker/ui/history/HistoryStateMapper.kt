package com.example.tracker.ui.history

import com.example.tracker.domain.model.WorkoutPlan
import com.example.tracker.domain.model.WorkoutSummary
import com.example.tracker.domain.progress.consistencyOf
import com.example.tracker.domain.progress.startOfWeek
import com.example.tracker.ui.common.toWorkoutSummaryUi
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** Maps plans and finished workouts to [HistoryUiState.Success]. Pure. */
internal class HistoryStateMapper(private val zone: ZoneId) {

    /**
     * [workouts] are newest first, already filtered to [selectedPlanId]; [allWorkouts] are every
     * finished workout, for the consistency calendar.
     */
    fun map(
        plans: List<WorkoutPlan>,
        selectedPlanId: String?,
        workouts: List<WorkoutSummary>,
        today: LocalDate,
        allWorkouts: List<WorkoutSummary> = workouts,
    ): HistoryUiState.Success =
        HistoryUiState.Success(
            filters = listOf(PlanFilterUi(planId = null, name = null)) + plans.map { PlanFilterUi(it.id, it.name, it.dayOfWeek) },
            selectedPlanId = selectedPlanId,
            months = workouts
                .map { it.toWorkoutSummaryUi(zone) }
                .groupBy { YearMonth.from(it.date) }
                .map { (month, items) -> MonthGroupUi(month, items) },
            consistency = consistency(today, allWorkouts),
        )

    /** The last [CALENDAR_WEEKS] weeks, this one included, and the streak. */
    private fun consistency(today: LocalDate, history: List<WorkoutSummary>): ConsistencyUi {
        val consistency = consistencyOf(history, today, zone)
        val firstMonday = today.startOfWeek().minusWeeks(CALENDAR_WEEKS - 1L)
        val weeks = (0 until CALENDAR_WEEKS).map { week ->
            (0 until DAYS_PER_WEEK).map { day ->
                val date = firstMonday.plusWeeks(week.toLong()).plusDays(day.toLong())
                CalendarDayUi(
                    date = date,
                    workoutCount = consistency.workoutsByDay[date] ?: 0,
                    isToday = date == today,
                    isFuture = date.isAfter(today),
                )
            }
        }
        return ConsistencyUi(consistency.weekStreak, weeks)
    }

    companion object {
        const val CALENDAR_WEEKS = 12
        private const val DAYS_PER_WEEK = 7
    }
}
