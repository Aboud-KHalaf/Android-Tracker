package com.example.tracker.ui.weight.components

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import com.example.tracker.R
import com.example.tracker.domain.weight.WeightSummary
import com.example.tracker.ui.common.chart.ChartModel
import com.example.tracker.ui.common.chart.LineChart
import com.example.tracker.ui.common.currentLocale
import com.example.tracker.ui.common.formatNumber
import com.example.tracker.ui.theme.spacing
import com.example.tracker.ui.theme.tabularNumbers
import com.example.tracker.ui.weight.weightChangeText
import com.example.tracker.ui.weight.weightText

/** Weight over the period as a line, with the change, lowest and highest below. */
@Composable
fun WeightChartCard(
    chart: ChartModel,
    summary: WeightSummary,
    modifier: Modifier = Modifier,
) {
    val locale = currentLocale()
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.lg),
        ) {
            LineChart(
                chart = chart,
                label = stringResource(R.string.weight_title),
                valueText = { weightText(it) },
                axisText = { formatNumber(it, locale) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.lg)) {
                Stat(stringResource(R.string.weight_change), weightChangeText(summary.changeKg), Modifier.weight(1f))
                Stat(stringResource(R.string.weight_lowest), weightText(summary.lowestKg), Modifier.weight(1f))
                Stat(stringResource(R.string.weight_highest), weightText(summary.highestKg), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.semantics(mergeDescendants = true) {}) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium.tabularNumbers())
    }
}
