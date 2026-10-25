package com.example.tracker.ui.stats

import com.example.tracker.domain.model.CustomStat
import com.example.tracker.domain.model.StatEntry
import java.time.LocalDate

/** Everything the Stats tab renders. */
sealed interface StatsUiState {
    data object Loading : StatsUiState
    data object Error : StatsUiState

    data class Success(
        val today: LocalDate,
        /** The user's stats, in the order they arranged them. */
        val stats: List<StatCardUi>,
    ) : StatsUiState
}

/** A stat's card: its latest value, the change since its first, and progress to its target. */
data class StatCardUi(
    val stat: CustomStat,
    val latest: StatEntry?,
    /** Null until there are entries on two days. */
    val changeSinceStart: Double?,
    /** From 0 to 1; null without a target or entries. */
    val progress: Float?,
)
