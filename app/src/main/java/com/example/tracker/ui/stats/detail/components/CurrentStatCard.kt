package com.example.tracker.ui.stats.detail.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.example.tracker.R
import com.example.tracker.domain.model.CustomStat
import com.example.tracker.domain.model.StatEntry
import com.example.tracker.ui.common.IconAvatar
import com.example.tracker.ui.common.RollingNumberText
import com.example.tracker.ui.common.currentLocale
import com.example.tracker.ui.common.longDate
import com.example.tracker.ui.stats.imageVector
import com.example.tracker.ui.stats.statValueText
import com.example.tracker.ui.theme.Dimens
import com.example.tracker.ui.theme.Motion
import com.example.tracker.ui.theme.spacing
import com.example.tracker.ui.theme.statColors
import com.example.tracker.ui.theme.tabularNumbers
import java.time.LocalDate
import kotlin.math.roundToInt

/** The latest value and when it was logged, progress to the target, and a button to log today. */
@Composable
fun CurrentStatCard(
    stat: CustomStat,
    latest: StatEntry?,
    progress: Float?,
    today: LocalDate,
    onLogToday: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.statColors[stat.color]
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
            ) {
                IconAvatar(icon = stat.icon.imageVector, containerColor = accent.container, contentColor = accent.content)
                Text(
                    text = stringResource(R.string.stat_current),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { heading() },
                )
                if (latest != null) {
                    TextButton(onClick = onLogToday, modifier = Modifier.heightIn(min = Dimens.minTouchTarget)) {
                        Text(stringResource(if (latest.date == today) R.string.stat_edit_today else R.string.stat_log_today))
                    }
                }
            }
            if (latest == null) {
                Text(
                    text = stringResource(R.string.stat_none_yet),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FilledTonalButton(onClick = onLogToday, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.stat_log_today))
                }
            } else {
                Column(modifier = Modifier.semantics(mergeDescendants = true) {}) {
                    RollingNumberText(
                        value = latest.value,
                        text = statValueText(latest.value, stat),
                        style = MaterialTheme.typography.displaySmall.tabularNumbers(),
                    )
                    Text(
                        text = if (latest.date == today) stringResource(R.string.stat_today) else latest.date.longDate(currentLocale()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            val target = stat.target
            if (target != null) {
                TargetProgress(progress, stringResource(R.string.stat_target, statValueText(target, stat)))
            }
        }
    }
}

@Composable
private fun TargetProgress(progress: Float?, targetText: String) {
    val fill = remember { Animatable(0f) }
    LaunchedEffect(progress) {
        fill.animateTo(progress ?: 0f, tween(Motion.DurationExtraLong, easing = Motion.Emphasized))
    }
    Column(
        modifier = Modifier.semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
    ) {
        Row {
            Text(targetText, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            if (progress != null) {
                Text(
                    text = stringResource(R.string.stat_percent, (progress * 100).roundToInt()),
                    style = MaterialTheme.typography.bodyLarge.tabularNumbers(),
                )
            }
        }
        LinearProgressIndicator(progress = { fill.value }, modifier = Modifier.fillMaxWidth())
    }
}
