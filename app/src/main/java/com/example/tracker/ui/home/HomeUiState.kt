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
        val programs: List<ProgramItemUi>,
        val recentWorkouts: List<WorkoutSummaryUi>,
        /** True while a workout is being started, to disable the start button. */
        val isStartingWorkout: Boolean = false,
    ) : HomeUiState
}

/** The highlighted card at the top of Home. */
sealed interface UpNextUi {
    /** A workout is already in progress. */
    data class Resume(val workoutId: String, val name: String, val startedAt: LocalTime) : UpNextUi

    /**
     * The active program's next training day. [scheduledOn] is the date it is planned for, or
     * null for a day without a fixed weekday.
     */
    data class Start(
        val planId: String,
        val name: String,
        val exerciseCount: Int,
        val lastDoneOn: LocalDate?,
        val scheduledOn: LocalDate?,
    ) : UpNextUi

    /** The active program has no day with exercises yet. */
    data class EmptyProgram(val programId: String, val name: String) : UpNextUi

    /** There is no program yet. */
    data object NoPrograms : UpNextUi
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

/** A program in the Programs list; [trainingDays] are its scheduled weekdays, Monday first. */
data class ProgramItemUi(
    val id: String,
    val name: String,
    val isActive: Boolean,
    val trainingDays: List<DayOfWeek>,
    val dayCount: Int,
)
