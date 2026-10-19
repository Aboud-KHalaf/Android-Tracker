package com.example.tracker.ui.stats

import com.example.tracker.domain.model.CustomStat
import com.example.tracker.domain.model.DailyNutrition
import com.example.tracker.domain.model.NutritionTargets
import com.example.tracker.domain.model.StatEntry
import com.example.tracker.domain.model.WeightEntry
import java.time.LocalDate

/** Everything the Stats tab renders. */
sealed interface StatsUiState {
    data object Loading : StatsUiState
    data object Error : StatsUiState

    data class Success(
        val today: LocalDate,
        val weight: WeightOverviewUi,
        val daily: DailyOverviewUi,
        /** The user's own stats, oldest first. */
        val stats: List<StatCardUi>,
    ) : StatsUiState
}

/** The latest weight ever, and how it moved over the last 30 days (null with fewer than two entries). */
data class WeightOverviewUi(val latest: WeightEntry?, val recentChangeKg: Double?)

/** What was logged today, against the daily targets. */
data class DailyOverviewUi(val today: DailyNutrition?, val targets: NutritionTargets)

/** A custom stat's card: its latest value, the change since its first, and progress to its target. */
data class StatCardUi(
    val stat: CustomStat,
    val latest: StatEntry?,
    /** Null until there are entries on two days. */
    val changeSinceStart: Double?,
    /** From 0 to 1; null without a target or entries. */
    val progress: Float?,
)
