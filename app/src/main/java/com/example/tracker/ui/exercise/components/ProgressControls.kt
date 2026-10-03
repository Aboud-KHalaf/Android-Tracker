package com.example.tracker.ui.exercise.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.tracker.R
import com.example.tracker.domain.progress.ProgressMetric
import com.example.tracker.domain.progress.TimeRange
import com.example.tracker.ui.exercise.description
import com.example.tracker.ui.exercise.label
import com.example.tracker.ui.theme.spacing

/** Weight / Reps / Volume. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetricSelector(
    metrics: List<ProgressMetric>,
    selected: ProgressMetric,
    onSelect: (ProgressMetric) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        metrics.forEachIndexed { index, metric ->
            SegmentedButton(
                selected = metric == selected,
                onClick = { onSelect(metric) },
                shape = SegmentedButtonDefaults.itemShape(index, metrics.size),
            ) { Text(metric.label()) }
        }
    }
}

/** 1M / 3M / 1Y / All. */
@Composable
fun RangeChips(
    selected: TimeRange,
    onSelect: (TimeRange) -> Unit,
    modifier: Modifier = Modifier,
) {
    val groupDescription = stringResource(R.string.range_group)
    Row(
        modifier = modifier.semantics { contentDescription = groupDescription },
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
    ) {
        TimeRange.entries.forEach { range ->
            val description = range.description()
            FilterChip(
                selected = range == selected,
                onClick = { onSelect(range) },
                label = { Text(range.label()) },
                modifier = Modifier.semantics { contentDescription = description },
            )
        }
    }
}
