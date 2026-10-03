package com.example.tracker.ui.home

import com.example.tracker.ui.common.SetValueUi
import com.example.tracker.ui.common.WorkoutSummaryUi
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime

/** Everything the Home screen renders. */
sealed interface HomeUiState {
    data object Loading : HomeUiState
    data object Error : HomeUiState

    data class Success(
        val today: LocalDate,
        val upNext: UpNextUi,
        val week: WeekUi,
        val plans: List<PlanItemUi>,
        val recentWorkouts: List<WorkoutSummaryUi>,
        /** True while a workout is being started, to disable the start button. */
        val isStartingWorkout: Boolean = false,
    ) : HomeUiState
}

/** The highlighted card at the top of Home. */
sealed interface UpNextUi {
    /** A workout is already in progress. */
    data class Resume(val workoutId: String, val name: String, val startedAt: LocalTime) : UpNextUi

    /** The suggested plan to train next. */
    data class Start(
        val planId: String,
        val name: String,
        val exerciseCount: Int,
        val lastDoneOn: LocalDate?,
    ) : UpNextUi

    /** No plan can be started yet. */
    data object NoPlans : UpNextUi
}

data class WeekUi(
    /** Monday to Sunday. */
    val days: List<WeekDayUi>,
    val workoutCount: Int,
    val totalDuration: Duration,
    /** The latest personal best set this week, if any. */
    val personalBest: PersonalBestUi?,
)

data class WeekDayUi(val dayOfWeek: DayOfWeek, val status: DayStatus)

enum class DayStatus { TRAINED, TODAY, IDLE }

data class PersonalBestUi(
    val exerciseId: String,
    val exerciseName: String,
    val value: SetValueUi,
    val achievedOn: DayOfWeek,
)

data class PlanItemUi(val id: String, val name: String, val exerciseCount: Int)
