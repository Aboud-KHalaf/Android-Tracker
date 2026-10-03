package com.example.tracker.ui.nutrition.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.example.tracker.R
import com.example.tracker.domain.model.DailyNutrition
import com.example.tracker.domain.model.NutritionTargets
import com.example.tracker.ui.common.currentLocale
import com.example.tracker.ui.common.longDate
import com.example.tracker.ui.nutrition.formatAmount
import com.example.tracker.ui.theme.Dimens
import com.example.tracker.ui.theme.spacing
import com.example.tracker.ui.theme.tabularNumbers
import java.time.LocalDate

/** Today's calories and protein against the targets, or a prompt to log them. */
@Composable
fun TodayCard(
    today: LocalDate,
    entry: DailyNutrition?,
    targets: NutritionTargets,
    onLogToday: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.lg),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.nutrition_today),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        text = today.longDate(currentLocale()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (entry != null) {
                    TextButton(onClick = onLogToday, modifier = Modifier.heightIn(min = Dimens.minTouchTarget)) {
                        Text(stringResource(R.string.nutrition_edit_today))
                    }
                }
            }
            if (entry == null) {
                Text(
                    text = stringResource(R.string.nutrition_today_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FilledTonalButton(onClick = onLogToday, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.nutrition_log_today))
                }
            } else {
                AmountProgress(
                    label = stringResource(R.string.nutrition_calories),
                    value = entry.calories,
                    target = targets.calories,
                    withTarget = R.string.nutrition_kcal_of_target,
                    withoutTarget = R.string.nutrition_kcal,
                )
                AmountProgress(
                    label = stringResource(R.string.nutrition_protein),
                    value = entry.proteinGrams,
                    target = targets.proteinGrams,
                    withTarget = R.string.nutrition_grams_of_target,
                    withoutTarget = R.string.nutrition_grams,
                )
            }
        }
    }
}

/** "Calories ...... 1,850 / 2,400 kcal" with a progress bar; just the amount without a target. */
@Composable
private fun AmountProgress(label: String, value: Int, target: Int?, withTarget: Int, withoutTarget: Int) {
    val locale = currentLocale()
    val amount = if (target != null) {
        stringResource(withTarget, formatAmount(value, locale), formatAmount(target, locale))
    } else {
        stringResource(withoutTarget, formatAmount(value, locale))
    }
    Column(
        modifier = Modifier.semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
    ) {
        Row {
            Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(amount, style = MaterialTheme.typography.bodyLarge.tabularNumbers())
        }
        if (target != null) {
            LinearProgressIndicator(
                progress = { (value.toFloat() / target).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
