package com.example.tracker.ui.exercise

import com.example.tracker.domain.model.ExerciseType
import com.example.tracker.domain.progress.ProgressMetric
import com.example.tracker.domain.progress.TimeRange
import com.example.tracker.ui.common.SetValueUi
import com.example.tracker.ui.exercise.chart.ChartModel
import java.time.LocalDate

/** Everything the Exercise details screen renders. */
sealed interface ExerciseDetailsUiState {
    data object Loading : ExerciseDetailsUiState
    data object Error : ExerciseDetailsUiState

    /** The exercise doesn't exist (any more). */
    data object NotFound : ExerciseDetailsUiState

    data class Success(
        val name: String,
        val type: ExerciseType,
        /** Metrics this exercise can be charted by; a picker is shown when there are several. */
        val metrics: List<ProgressMetric>,
        val metric: ProgressMetric,
        val range: TimeRange,
        /** The chart and trend for [metric] in [range]; null when no session is in range. */
        val progress: ProgressUi?,
        /** All-time best; null before the first session. */
        val personalBest: PersonalBestSummaryUi?,
        val sessionCount: Int,
        val firstSessionOn: LocalDate?,
        /** Every session, newest first. */
        val sessions: List<SessionRowUi>,
    ) : ExerciseDetailsUiState {
        val recentSessions: List<SessionRowUi> get() = sessions.take(RECENT_SESSION_COUNT)
    }

    companion object {
        const val RECENT_SESSION_COUNT = 3
    }
}

data class ProgressUi(
    val chart: ChartModel,
    /** Last value minus first value in range. */
    val change: Double,
    /** Date of the first session in range. */
    val since: LocalDate,
) {
    val trend: Trend
        get() = when {
            change > 0 -> Trend.UP
            change < 0 -> Trend.DOWN
            else -> Trend.FLAT
        }
}

enum class Trend { UP, DOWN, FLAT }

data class PersonalBestSummaryUi(val value: SetValueUi, val achievedOn: LocalDate)

data class SessionRowUi(
    val workoutId: String,
    val date: LocalDate,
    /** Top set, or longest hold. */
    val best: SetValueUi?,
    val sets: List<SetValueUi>,
    val isPersonalBest: Boolean,
)
