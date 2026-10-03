package com.example.tracker.ui.exercise.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingFlat
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.unit.dp
import com.example.tracker.R
import com.example.tracker.domain.progress.ProgressMetric
import com.example.tracker.ui.exercise.ProgressUi
import com.example.tracker.ui.exercise.Trend
import com.example.tracker.ui.exercise.changeText
import com.example.tracker.ui.exercise.sinceText
import com.example.tracker.ui.theme.spacing
import com.example.tracker.ui.theme.tabularNumbers

/** "Improving / +10 kg / Top set weight since Aug 1". */
@Composable
fun TrendHeadline(progress: ProgressUi, metric: ProgressMetric, modifier: Modifier = Modifier) {
    val (icon, label) = when (progress.trend) {
        Trend.UP -> Icons.AutoMirrored.Outlined.TrendingUp to stringResource(R.string.trend_up)
        Trend.DOWN -> Icons.AutoMirrored.Outlined.TrendingDown to stringResource(R.string.trend_down)
        Trend.FLAT -> Icons.AutoMirrored.Outlined.TrendingFlat to stringResource(R.string.trend_flat)
    }
    val trendColor = if (progress.trend == Trend.UP) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
        ) {
            Icon(icon, contentDescription = null, tint = trendColor, modifier = Modifier.size(20.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, color = trendColor)
        }
        Text(
            text = metric.changeText(progress.change),
            style = MaterialTheme.typography.displaySmall.tabularNumbers(),
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = metric.sinceText(progress.since),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
