package com.example.tracker.ui.stats.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.tracker.R
import com.example.tracker.ui.common.currentLocale
import com.example.tracker.ui.common.longDate
import com.example.tracker.ui.stats.StatCardUi
import com.example.tracker.ui.stats.imageVector
import com.example.tracker.ui.stats.statChangeText
import com.example.tracker.ui.stats.statValueText
import com.example.tracker.ui.theme.statColors
import java.time.LocalDate
import kotlin.math.roundToInt

/** One of the user's stats: the latest value, the change since the first, progress to the target, and a log button. */
@Composable
fun CustomStatCard(
    card: StatCardUi,
    today: LocalDate,
    onClick: () -> Unit,
    onLog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val stat = card.stat
    val latest = card.latest
    StatCard(
        title = stat.name,
        icon = stat.icon.imageVector,
        accent = MaterialTheme.statColors[stat.color],
        value = latest?.let { statValueText(it.value, stat) },
        supporting = if (latest == null) {
            stringResource(R.string.stats_not_logged)
        } else {
            listOfNotNull(
                dayText(latest.date, today),
                card.changeSinceStart?.let { stringResource(R.string.stat_since_start, statChangeText(it, stat)) },
            ).joinToString(SEPARATOR)
        },
        progress = card.progress,
        progressLabel = card.progress?.let { progress ->
            stat.target?.let { stringResource(R.string.stat_target_progress, (progress * 100).roundToInt(), statValueText(it, stat)) }
        },
        trailing = {
            IconButton(onClick = onLog) {
                Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.stat_log_named, stat.name))
            }
        },
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
private fun dayText(date: LocalDate, today: LocalDate): String =
    if (date == today) stringResource(R.string.stat_today) else date.longDate(currentLocale())

private const val SEPARATOR = " · "
