package com.example.tracker.ui.summary

import com.example.tracker.ui.common.SetValueUi
import java.time.Duration
import java.time.LocalDate

/** Everything the workout summary renders. */
sealed interface WorkoutSummaryUiState {
    data object Loading : WorkoutSummaryUiState
    data object Error : WorkoutSummaryUiState

    /** The workout doesn't exist, was discarded, or isn't finished. */
    data object NotFound : WorkoutSummaryUiState

    data class Success(
        val name: String,
        val date: LocalDate,
        /** Shown right after finishing, rather than opened from history. */
        val justFinished: Boolean,
        val duration: Duration,
        val setCount: Int,
        val volumeKg: Double,
        /** Volume against the previous workout of the same day; null without one. */
        val volumeChangeKg: Double?,
        val personalBests: List<PersonalBestUi>,
        val exercises: List<ExerciseSummaryUi>,
    ) : WorkoutSummaryUiState
}

data class PersonalBestUi(val exerciseName: String, val value: SetValueUi)

data class ExerciseSummaryUi(
    val exerciseId: String,
    val name: String,
    val sets: List<SetValueUi>,
    val isPersonalBest: Boolean,
)
