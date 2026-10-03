package com.example.tracker.ui.home

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.tracker.ui.common.SetValueUi
import com.example.tracker.ui.common.WorkoutSummaryUi
import com.example.tracker.ui.theme.TrackerTheme
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate

private val today = LocalDate.of(2026, 10, 3)

private val previewState = HomeUiState.Success(
    today = today,
    upNext = UpNextUi.Start("push", "Push Day", 5, LocalDate.of(2026, 9, 28)),
    week = WeekUi(
        days = DayOfWeek.entries.map { day ->
            val status = when (day) {
                DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY -> DayStatus.TRAINED
                DayOfWeek.SATURDAY -> DayStatus.TODAY
                else -> DayStatus.IDLE
            }
            WeekDayUi(day, status)
        },
        workoutCount = 3,
        totalDuration = Duration.ofMinutes(155),
        personalBest = PersonalBestUi("bench", "Bench Press", SetValueUi.WeightReps(50.0, 8), DayOfWeek.MONDAY),
    ),
    plans = listOf(
        PlanItemUi("push", "Push Day", 5),
        PlanItemUi("pull", "Pull Day", 5),
        PlanItemUi("legs", "Legs & Core", 6),
    ),
    recentWorkouts = listOf(
        WorkoutSummaryUi("w3", "Legs & Core", LocalDate.of(2026, 10, 2), 6, Duration.ofMinutes(55), 0),
        WorkoutSummaryUi("w2", "Pull Day", LocalDate.of(2026, 9, 30), 5, Duration.ofMinutes(48), 1),
    ),
)

private val emptyState = HomeUiState.Success(
    today = today,
    upNext = UpNextUi.NoPlans,
    week = WeekUi(
        days = DayOfWeek.entries.map { WeekDayUi(it, if (it == DayOfWeek.SATURDAY) DayStatus.TODAY else DayStatus.IDLE) },
        workoutCount = 0,
        totalDuration = Duration.ZERO,
        personalBest = null,
    ),
    plans = emptyList(),
    recentWorkouts = emptyList(),
)

@Preview(name = "Light", showBackground = true, heightDp = 1140)
@Preview(name = "Dark", showBackground = true, heightDp = 1140, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HomeScreenPreview() {
    TrackerTheme { HomeScreen(uiState = previewState, actions = HomeActions()) }
}

@Preview(name = "Empty", showBackground = true)
@Composable
private fun HomeScreenEmptyPreview() {
    TrackerTheme { HomeScreen(uiState = emptyState, actions = HomeActions()) }
}

@Preview(name = "Error", showBackground = true)
@Composable
private fun HomeScreenErrorPreview() {
    TrackerTheme { HomeScreen(uiState = HomeUiState.Error, actions = HomeActions()) }
}
