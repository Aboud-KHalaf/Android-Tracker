package com.example.tracker.ui.nutrition.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.example.tracker.R
import com.example.tracker.domain.nutrition.NutritionSummary
import com.example.tracker.ui.common.currentLocale
import com.example.tracker.ui.nutrition.formatAmount
import com.example.tracker.ui.theme.spacing
import com.example.tracker.ui.theme.tabularNumbers

/** Daily averages over the period, how many days were logged, and how often targets were hit. */
@Composable
fun PeriodSummaryCard(
    summary: NutritionSummary,
    rangeDayCount: Int,
    modifier: Modifier = Modifier,
) {
    val locale = currentLocale()
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
        ) {
            Text(
                text = stringResource(R.string.nutrition_daily_average),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.lg)) {
                Average(
                    label = stringResource(R.string.nutrition_calories),
                    value = stringResource(R.string.nutrition_kcal, formatAmount(summary.averageCalories, locale)),
                    modifier = Modifier.weight(1f),
                )
                Average(
                    label = stringResource(R.string.nutrition_protein),
                    value = stringResource(R.string.nutrition_grams, formatAmount(summary.averageProteinGrams, locale)),
                    modifier = Modifier.weight(1f),
                )
            }
            summary.averageSteps?.let { steps ->
                Average(
                    label = stringResource(R.string.nutrition_steps),
                    value = stringResource(R.string.nutrition_steps_value, formatAmount(steps, locale)),
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Detail(pluralStringResource(R.plurals.nutrition_logged_days, rangeDayCount, summary.loggedDays, rangeDayCount))
            summary.caloriesOnTargetDays?.let { hits ->
                Detail(pluralStringResource(R.plurals.nutrition_calories_on_target, summary.loggedDays, hits, summary.loggedDays))
            }
            summary.proteinOnTargetDays?.let { hits ->
                Detail(pluralStringResource(R.plurals.nutrition_protein_on_target, summary.loggedDays, hits, summary.loggedDays))
            }
        }
    }
}

@Composable
private fun Average(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.semantics(mergeDescendants = true) {}) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleLarge.tabularNumbers())
    }
}

@Composable
private fun Detail(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium.tabularNumbers(), color = MaterialTheme.colorScheme.onSurfaceVariant)
}
