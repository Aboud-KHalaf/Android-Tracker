package com.example.tracker.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.tracker.R
import com.example.tracker.ui.common.IconAvatar
import com.example.tracker.ui.common.currentLocale
import com.example.tracker.ui.common.formatDuration
import com.example.tracker.ui.common.fullName
import com.example.tracker.ui.common.narrowName
import com.example.tracker.ui.common.shortName
import com.example.tracker.ui.common.text
import com.example.tracker.ui.home.DayStatus
import com.example.tracker.ui.home.PersonalBestUi
import com.example.tracker.ui.home.WeekDayUi
import com.example.tracker.ui.home.WeekUi
import com.example.tracker.ui.theme.spacing
import com.example.tracker.ui.theme.tabularNumbers

private val DayIndicatorSize = 32.dp
private val DayCheckSize = 18.dp
private val TodayBorderWidth = 2.dp

/** "This week": totals, a Monday-to-Sunday strip, and this week's latest personal best. */
@Composable
fun WeekCard(
    week: WeekUi,
    onOpenExerciseProgress: (exerciseId: String) -> Unit,
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
            WeekTotals(week)
            WeekStrip(week.days)
            week.personalBest?.let { best ->
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                PersonalBestRow(best, onOpen = { onOpenExerciseProgress(best.exerciseId) })
            }
        }
    }
}

@Composable
private fun WeekTotals(week: WeekUi) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.home_this_week),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        Text(
            text = pluralStringResource(R.plurals.workout_count, week.workoutCount, week.workoutCount) +
                stringResource(R.string.separator_dot) + formatDuration(week.totalDuration),
            style = MaterialTheme.typography.bodyMedium.tabularNumbers(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun WeekStrip(days: List<WeekDayUi>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
    ) {
        days.forEach { day -> WeekDay(day, Modifier.weight(1f)) }
    }
}

@Composable
private fun WeekDay(day: WeekDayUi, modifier: Modifier = Modifier) {
    val locale = currentLocale()
    val dayName = day.dayOfWeek.fullName(locale)
    val description = when (day.status) {
        DayStatus.TRAINED -> stringResource(R.string.home_day_trained, dayName)
        DayStatus.TODAY -> stringResource(R.string.home_day_today, dayName)
        DayStatus.IDLE -> dayName
    }
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
    ) {
        Text(
            text = day.dayOfWeek.narrowName(locale),
            style = MaterialTheme.typography.labelMedium,
            color = if (day.status == DayStatus.TODAY) colors.primary else colors.onSurfaceVariant,
        )
        val indicator = Modifier.size(DayIndicatorSize)
        when (day.status) {
            DayStatus.TRAINED -> Box(indicator.background(colors.primary, CircleShape), Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = colors.onPrimary,
                    modifier = Modifier.size(DayCheckSize),
                )
            }

            DayStatus.TODAY -> Box(indicator.border(TodayBorderWidth, colors.primary, CircleShape))
            DayStatus.IDLE -> Box(indicator.background(colors.surfaceContainerHighest, CircleShape))
        }
    }
}

@Composable
private fun PersonalBestRow(best: PersonalBestUi, onOpen: () -> Unit) {
    val separator = stringResource(R.string.separator_dot)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
    ) {
        IconAvatar(
            icon = Icons.Outlined.EmojiEvents,
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(text = stringResource(R.string.home_new_personal_best), style = MaterialTheme.typography.titleSmall)
            Text(
                text = best.exerciseName + separator + best.value.text() + separator +
                    best.achievedOn.shortName(currentLocale()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onOpen) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = stringResource(R.string.home_view_progress, best.exerciseName),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
