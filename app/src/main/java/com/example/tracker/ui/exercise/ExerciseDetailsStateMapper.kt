package com.example.tracker.ui.exercise

import com.example.tracker.domain.model.Exercise
import com.example.tracker.domain.model.ExerciseSession
import com.example.tracker.domain.model.LoggedSet
import com.example.tracker.domain.model.PersonalBest
import com.example.tracker.domain.progress.ProgressMetric
import com.example.tracker.domain.progress.TimeRange
import com.example.tracker.domain.progress.currentPersonalBest
import com.example.tracker.domain.progress.findPersonalBests
import com.example.tracker.domain.progress.stats
import com.example.tracker.ui.common.SetValueUi
import com.example.tracker.ui.common.chart.ChartModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Maps an exercise and its sessions to [ExerciseDetailsUiState]. Pure. */
internal class ExerciseDetailsStateMapper(private val zone: ZoneId) {

    fun map(
        exercise: Exercise?,
        sessions: List<ExerciseSession>,
        selectedMetric: ProgressMetric?,
        range: TimeRange,
        now: Instant,
    ): ExerciseDetailsUiState {
        if (exercise == null) return ExerciseDetailsUiState.NotFound
        val metrics = ProgressMetric.forType(exercise.type)
        val metric = selectedMetric?.takeIf { it in metrics } ?: metrics.first()
        val ordered = sessions.sortedBy { it.finishedAt }
        val bestWorkoutIds = findPersonalBests(ordered).map { it.workoutId }.toSet()

        return ExerciseDetailsUiState.Success(
            name = exercise.name,
            type = exercise.type,
            metrics = metrics,
            metric = metric,
            range = range,
            progress = progress(ordered.filter { range.contains(it.finishedAt, now, zone) }, metric),
            personalBest = currentPersonalBest(ordered)?.toSummary(),
            sessionCount = ordered.size,
            firstSessionOn = ordered.firstOrNull()?.finishedAt?.toDate(),
            sessions = ordered.asReversed().map { it.toRow(isPersonalBest = it.workoutId in bestWorkoutIds) },
        )
    }

    private fun progress(sessions: List<ExerciseSession>, metric: ProgressMetric): ProgressUi? {
        val values = sessions.mapNotNull { session ->
            metric.valueOf(session)?.let { session.finishedAt.toDate() to it }
        }
        val chart = ChartModel.build(values) ?: return null
        return ProgressUi(chart = chart, change = chart.last.value - chart.first.value, since = chart.first.date)
    }

    private fun PersonalBest.toSummary(): PersonalBestSummaryUi? {
        val value = when {
            weightKg != null && reps != null -> SetValueUi.WeightReps(weightKg, reps)
            durationSeconds != null -> SetValueUi.Hold(durationSeconds)
            else -> return null
        }
        return PersonalBestSummaryUi(value, achievedAt.toDate())
    }

    private fun ExerciseSession.toRow(isPersonalBest: Boolean): SessionRowUi {
        val stats = stats()
        val topWeight = stats.topWeightKg
        val topReps = stats.topSetReps
        val best = when {
            topWeight != null && topReps != null -> SetValueUi.WeightReps(topWeight, topReps)
            stats.longestHoldSeconds != null -> SetValueUi.Hold(stats.longestHoldSeconds)
            else -> null
        }
        return SessionRowUi(
            workoutId = workoutId,
            date = finishedAt.toDate(),
            best = best,
            sets = sets.mapNotNull { it.toValue() },
            isPersonalBest = isPersonalBest,
        )
    }

    private fun LoggedSet.toValue(): SetValueUi? = when {
        weightKg != null && reps != null -> SetValueUi.WeightReps(weightKg, reps)
        durationSeconds != null -> SetValueUi.Hold(durationSeconds)
        else -> null
    }

    private fun Instant.toDate(): LocalDate = atZone(zone).toLocalDate()
}
