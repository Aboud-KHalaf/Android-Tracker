package com.example.tracker.ui.program

import java.time.DayOfWeek

/** Everything the Program screen renders. */
sealed interface ProgramUiState {
    data object Loading : ProgramUiState
    data object Error : ProgramUiState

    /** The program doesn't exist (any more), e.g. it was deleted. */
    data object NotFound : ProgramUiState

    data class Success(
        val name: String,
        val isActive: Boolean,
        /** Scheduled days Monday to Sunday, then days without a fixed weekday. */
        val days: List<ProgramDayUi>,
        /** A day with exercises the user asked to remove; they confirm first. */
        val removalToConfirm: ProgramDayUi? = null,
    ) : ProgramUiState {
        val trainingDays: Set<DayOfWeek> get() = days.mapNotNullTo(mutableSetOf()) { it.dayOfWeek }
    }
}

/** A training day. [id] is the day's plan id; [dayOfWeek] is null for "any day". */
data class ProgramDayUi(
    val id: String,
    val name: String,
    val dayOfWeek: DayOfWeek?,
    val exerciseCount: Int,
)
