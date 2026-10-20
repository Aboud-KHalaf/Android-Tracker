package com.example.tracker.ui.stats.detail.components

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
import com.example.tracker.domain.model.CustomStat
import com.example.tracker.domain.stats.StatSummary
import com.example.tracker.ui.common.chart.ChartModel
import com.example.tracker.ui.common.chart.LineChart
import com.example.tracker.ui.common.currentLocale
import com.example.tracker.ui.common.formatNumber
import com.example.tracker.ui.stats.statChangeText
import com.example.tracker.ui.stats.statValueText
import com.example.tracker.ui.theme.spacing
import com.example.tracker.ui.theme.tabularNumbers

/** The stat over the period as a line, with the change, lowest and highest below. */
@Composable
fun StatChartCard(
    stat: CustomStat,
    chart: ChartModel,
    summary: StatSummary,
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
                label = stat.name,
                valueText = { statValueText(it, stat) },
                axisText = { formatNumber(it, locale) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.lg)) {
                Figure(stringResource(R.string.stat_change), statChangeText(summary.change, stat), Modifier.weight(1f))
                Figure(stringResource(R.string.stat_lowest), statValueText(summary.lowest, stat), Modifier.weight(1f))
                Figure(stringResource(R.string.stat_highest), statValueText(summary.highest, stat), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Figure(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.semantics(mergeDescendants = true) {}) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium.tabularNumbers())
    }
}
