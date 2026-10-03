package com.example.tracker.ui.history

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.tracker.ui.common.WorkoutSummaryUi
import com.example.tracker.ui.theme.TrackerTheme
import java.time.Duration
import java.time.LocalDate
import java.time.YearMonth

private fun workout(id: String, name: String, date: LocalDate, exercises: Int, minutes: Long, prs: Int = 0) =
    WorkoutSummaryUi(id, name, date, exercises, Duration.ofMinutes(minutes), prs)

private val previewState = HistoryUiState.Success(
    filters = listOf(
        PlanFilterUi(null, null),
        PlanFilterUi("push", "Push"),
        PlanFilterUi("pull", "Pull"),
        PlanFilterUi("legs", "Legs & Core"),
    ),
    selectedPlanId = null,
    months = listOf(
        MonthGroupUi(YearMonth.of(2026, 10), listOf(workout("w9", "Legs & Core", LocalDate.of(2026, 10, 2), 6, 55))),
        MonthGroupUi(
            YearMonth.of(2026, 9),
            listOf(
                workout("w8", "Pull Day", LocalDate.of(2026, 9, 30), 5, 48, prs = 1),
                workout("w7", "Push Day", LocalDate.of(2026, 9, 28), 5, 52, prs = 1),
                workout("w6", "Legs & Core", LocalDate.of(2026, 9, 25), 6, 57),
                workout("w5", "Pull Day", LocalDate.of(2026, 9, 23), 5, 46),
            ),
        ),
    ),
)

@Preview(name = "Light", showBackground = true, heightDp = 764)
@Preview(name = "Dark", showBackground = true, heightDp = 764, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HistoryScreenPreview() {
    TrackerTheme { HistoryScreen(uiState = previewState, onSelectPlan = {}, onRetry = {}) }
}

@Preview(name = "Empty filter", showBackground = true, heightDp = 500)
@Composable
private fun HistoryScreenEmptyPreview() {
    TrackerTheme {
        HistoryScreen(uiState = previewState.copy(selectedPlanId = "legs", months = emptyList()), onSelectPlan = {}, onRetry = {})
    }
}
