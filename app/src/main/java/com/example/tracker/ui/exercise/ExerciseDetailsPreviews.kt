package com.example.tracker.ui.exercise

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.tracker.domain.model.ExerciseType
import com.example.tracker.domain.progress.ProgressMetric
import com.example.tracker.domain.progress.TimeRange
import com.example.tracker.ui.common.SetValueUi
import com.example.tracker.ui.exercise.chart.ChartModel
import com.example.tracker.ui.theme.TrackerTheme
import java.time.LocalDate

private val firstDay = LocalDate.of(2026, 8, 10)
private val topSets = listOf(40.0, 40.0, 42.5, 42.5, 45.0, 45.0, 45.0, 50.0)

private val previewSessions = topSets.indices.reversed().map { i ->
    val top = topSets[i]
    SessionRowUi(
        workoutId = "w$i",
        date = firstDay.plusWeeks(i.toLong()),
        best = SetValueUi.WeightReps(top, 8),
        sets = listOf(SetValueUi.WeightReps(top - 5, 10), SetValueUi.WeightReps(top, 8), SetValueUi.WeightReps(top, 8)),
        isPersonalBest = i > 0 && top > topSets.take(i).max(),
    )
}

private val previewState = ExerciseDetailsUiState.Success(
    name = "Bench Press",
    type = ExerciseType.WEIGHT_REPS,
    metrics = ProgressMetric.forType(ExerciseType.WEIGHT_REPS),
    metric = ProgressMetric.WEIGHT,
    range = TimeRange.THREE_MONTHS,
    progress = ProgressUi(
        chart = ChartModel.build(topSets.mapIndexed { i, v -> firstDay.plusWeeks(i.toLong()) to v })!!,
        change = 10.0,
        since = firstDay,
    ),
    personalBest = PersonalBestSummaryUi(SetValueUi.WeightReps(50.0, 8), LocalDate.of(2026, 9, 28)),
    sessionCount = topSets.size,
    firstSessionOn = firstDay,
    sessions = previewSessions,
)

@Preview(name = "Light", showBackground = true, heightDp = 980)
@Preview(name = "Dark", showBackground = true, heightDp = 980, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ExerciseDetailsPreview() {
    TrackerTheme { ExerciseDetailsScreen(uiState = previewState, actions = ExerciseDetailsActions()) }
}

@Preview(name = "History tab", showBackground = true, heightDp = 844)
@Composable
private fun ExerciseDetailsHistoryPreview() {
    TrackerTheme {
        ExerciseDetailsScreen(uiState = previewState, actions = ExerciseDetailsActions(), initialTab = ExerciseTab.HISTORY)
    }
}

@Preview(name = "No sessions", showBackground = true, heightDp = 600)
@Composable
private fun ExerciseDetailsEmptyPreview() {
    TrackerTheme {
        ExerciseDetailsScreen(
            uiState = previewState.copy(progress = null, personalBest = null, sessionCount = 0, firstSessionOn = null, sessions = emptyList()),
            actions = ExerciseDetailsActions(),
        )
    }
}
