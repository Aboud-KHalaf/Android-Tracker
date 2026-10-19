package com.example.tracker.ui.stats.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.tracker.R
import com.example.tracker.domain.model.StatColor
import com.example.tracker.ui.common.currentLocale
import com.example.tracker.ui.common.longDate
import com.example.tracker.ui.nutrition.formatAmount
import com.example.tracker.ui.stats.DailyOverviewUi
import com.example.tracker.ui.stats.StatCardUi
import com.example.tracker.ui.stats.WeightOverviewUi
import com.example.tracker.ui.stats.imageVector
import com.example.tracker.ui.stats.statChangeText
import com.example.tracker.ui.stats.statValueText
import com.example.tracker.ui.theme.statColors
import com.example.tracker.ui.weight.weightChangeText
import com.example.tracker.ui.weight.weightText
import java.time.LocalDate
import kotlin.math.roundToInt

/** Weight as a stat: the latest weight, when it was logged, and the change over 30 days. */
@Composable
fun WeightStatCard(weight: WeightOverviewUi, today: LocalDate, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val latest = weight.latest
    StatCard(
        title = stringResource(R.string.weight_title),
        icon = Icons.Outlined.MonitorWeight,
        accent = MaterialTheme.statColors[StatColor.BLUE],
        value = latest?.let { weightText(it.weightKg) },
        supporting = if (latest == null) {
            stringResource(R.string.stats_not_logged)
        } else {
            listOfNotNull(
                dayText(latest.date, today),
                weight.recentChangeKg?.let { stringResource(R.string.stats_weight_recent_change, weightChangeText(it)) },
            ).joinToString(SEPARATOR)
        },
        onClick = onClick,
        modifier = modifier,
    )
}

/** The daily log as a stat: today's calories against the target, with protein and steps below. */
@Composable
fun DailyStatCard(daily: DailyOverviewUi, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val locale = currentLocale()
    val entry = daily.today
    val calorieTarget = daily.targets.calories
    StatCard(
        title = stringResource(R.string.nutrition_title),
        icon = Icons.Outlined.Today,
        accent = MaterialTheme.statColors[StatColor.GREEN],
        value = entry?.let {
            if (calorieTarget != null) {
                stringResource(R.string.nutrition_kcal_of_target, formatAmount(it.calories, locale), formatAmount(calorieTarget, locale))
            } else {
                stringResource(R.string.nutrition_kcal, formatAmount(it.calories, locale))
            }
        },
        supporting = if (entry == null) {
            stringResource(R.string.stats_daily_none)
        } else {
            listOfNotNull(
                stringResource(R.string.stats_daily_protein, formatAmount(entry.proteinGrams, locale)),
                entry.steps?.let { stringResource(R.string.nutrition_steps_value, formatAmount(it, locale)) },
            ).joinToString(SEPARATOR)
        },
        progress = if (entry != null && calorieTarget != null && calorieTarget > 0) {
            (entry.calories.toFloat() / calorieTarget).coerceIn(0f, 1f)
        } else {
            null
        },
        onClick = onClick,
        modifier = modifier,
    )
}

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
    if (date == today) stringResource(R.string.weight_today) else date.longDate(currentLocale())

private const val SEPARATOR = " · "
