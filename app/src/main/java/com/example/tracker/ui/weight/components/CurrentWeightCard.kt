package com.example.tracker.ui.weight.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
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
import com.example.tracker.domain.model.WeightEntry
import com.example.tracker.ui.common.currentLocale
import com.example.tracker.ui.common.longDate
import com.example.tracker.ui.theme.Dimens
import com.example.tracker.ui.theme.spacing
import com.example.tracker.ui.theme.tabularNumbers
import com.example.tracker.ui.weight.weightText
import java.time.LocalDate

/** The latest weight and when it was logged, with a button to log today's; or a prompt to start. */
@Composable
fun CurrentWeightCard(
    latest: WeightEntry?,
    today: LocalDate,
    onLogToday: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.weight_current),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { heading() },
                )
                if (latest != null) {
                    TextButton(onClick = onLogToday, modifier = Modifier.heightIn(min = Dimens.minTouchTarget)) {
                        Text(stringResource(if (latest.date == today) R.string.weight_edit_today else R.string.weight_log_today))
                    }
                }
            }
            if (latest == null) {
                Text(
                    text = stringResource(R.string.weight_none_yet),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FilledTonalButton(onClick = onLogToday, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.weight_log_today))
                }
            } else {
                Column(modifier = Modifier.semantics(mergeDescendants = true) {}) {
                    Text(weightText(latest.weightKg), style = MaterialTheme.typography.displaySmall.tabularNumbers())
                    Text(
                        text = if (latest.date == today) stringResource(R.string.weight_today) else latest.date.longDate(currentLocale()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
