package com.example.tracker.ui.weight

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.tracker.domain.model.DatePeriod
import com.example.tracker.domain.model.WeightEntry
import com.example.tracker.ui.theme.TrackerTheme
import java.time.LocalDate

private val today = LocalDate.of(2026, 10, 18)
private val previewEntries = listOf(78.4, 78.9, 78.8, 79.0, 79.4, 79.3, 79.8, 79.7, 79.9, 80.4, 80.2, 80.6)
    .mapIndexed { i, kg -> WeightEntry(today.minusDays(i * 2L), kg) }

private val previewState = WeightStateMapper().map(today, previewEntries.first(), DatePeriod.Last30Days, previewEntries)

@Preview(name = "Light", showBackground = true, heightDp = 1300)
@Preview(name = "Dark", showBackground = true, heightDp = 1300, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WeightScreenPreview() {
    TrackerTheme { WeightScreen(uiState = previewState, onOpenEntry = {}, onSelectPeriod = {}, onRetry = {}) }
}

@Preview(name = "Nothing logged", showBackground = true, heightDp = 700)
@Composable
private fun WeightScreenEmptyPreview() {
    TrackerTheme {
        WeightScreen(
            uiState = WeightStateMapper().map(today, null, DatePeriod.Last30Days, emptyList()),
            onOpenEntry = {},
            onSelectPeriod = {},
            onRetry = {},
        )
    }
}
