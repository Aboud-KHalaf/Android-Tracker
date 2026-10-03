package com.example.tracker.ui.nutrition

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.tracker.domain.model.DailyNutrition
import com.example.tracker.domain.model.DatePeriod
import com.example.tracker.domain.model.NutritionTargets
import com.example.tracker.ui.theme.TrackerTheme
import java.time.LocalDate

private val today = LocalDate.of(2026, 10, 18)
private val targets = NutritionTargets(calories = 2400, proteinGrams = 160)
private val previewDays = listOf(
    2300 to 150, 2550 to 172, 2100 to 140, 2450 to 165, 1900 to 120, 2600 to 180, 2350 to 158,
    2200 to 161, 2480 to 170, 2700 to 145, 2000 to 130, 2400 to 166,
).mapIndexed { i, (calories, protein) -> DailyNutrition(today.minusDays(i.toLong() + (i / 4)), calories, protein) }

private val previewState = NutritionStateMapper().map(
    today = today,
    todayEntry = previewDays.first(),
    period = DatePeriod.ThisMonth,
    metric = NutritionMetric.CALORIES,
    days = previewDays,
    targets = targets,
)

@Preview(name = "Light", showBackground = true, heightDp = 1400)
@Preview(name = "Dark", showBackground = true, heightDp = 1400, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun NutritionScreenPreview() {
    TrackerTheme {
        NutritionScreen(
            uiState = previewState,
            onOpenDay = {},
            onSaveTargets = {},
            onSelectPeriod = {},
            onSelectMetric = {},
            onRetry = {},
        )
    }
}

@Preview(name = "Nothing logged", showBackground = true, heightDp = 700)
@Composable
private fun NutritionScreenEmptyPreview() {
    TrackerTheme {
        NutritionScreen(
            uiState = NutritionStateMapper().map(today, null, DatePeriod.ThisMonth, NutritionMetric.CALORIES, emptyList(), NutritionTargets()),
            onOpenDay = {},
            onSaveTargets = {},
            onSelectPeriod = {},
            onSelectMetric = {},
            onRetry = {},
        )
    }
}
