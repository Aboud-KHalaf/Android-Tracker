package com.example.tracker.ui.history.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.tracker.R
import com.example.tracker.ui.common.currentLocale
import com.example.tracker.ui.common.narrowName
import com.example.tracker.ui.history.CalendarDayUi
import com.example.tracker.ui.history.ConsistencyUi
import com.example.tracker.ui.theme.spacing
import java.time.DayOfWeek

private val CellGap = 3.dp
private val CellShape = RoundedCornerShape(3.dp)
private val TodayBorderWidth = 1.5.dp
private val LegendCellSize = 10.dp
private val DayLabelWidth = 16.dp

/** "Consistency": the week streak and a calendar of the last weeks, one column per week. */
@Composable
fun ConsistencyCard(consistency: ConsistencyUi, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
        ) {
            Text(
                stringResource(R.string.history_consistency),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
            ) {
                Icon(
                    Icons.Outlined.LocalFireDepartment,
                    contentDescription = null,
                    tint = if (consistency.weekStreak > 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline,
                )
                Column {
                    Text(
                        text = if (consistency.weekStreak > 0) {
                            pluralStringResource(R.plurals.home_week_streak, consistency.weekStreak, consistency.weekStreak)
                        } else {
                            stringResource(R.string.home_no_streak)
                        },
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = pluralStringResource(
                            R.plurals.home_days_trained,
                            consistency.trainedDayCount,
                            consistency.trainedDayCount,
                            consistency.weeks.size,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (consistency.weeks.isNotEmpty()) TrainingCalendar(consistency.weeks)
        }
    }
}

/** The grid itself; decorative, since the text above says what it shows. */
@Composable
private fun TrainingCalendar(weeks: List<List<CalendarDayUi>>) {
    val locale = currentLocale()
    Column(
        modifier = Modifier.clearAndSetSemantics {},
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            // Square cells that share the width left beside the weekday labels.
            val cellSize = (maxWidth - DayLabelWidth - CellGap * weeks.size) / weeks.size
            Row(horizontalArrangement = Arrangement.spacedBy(CellGap)) {
                // Monday, Wednesday and Friday label their rows, as calendars usually do.
                Column(verticalArrangement = Arrangement.spacedBy(CellGap)) {
                    DayOfWeek.entries.forEach { day ->
                        Box(Modifier.size(DayLabelWidth, cellSize), contentAlignment = Alignment.CenterStart) {
                            if (day == DayOfWeek.MONDAY || day == DayOfWeek.WEDNESDAY || day == DayOfWeek.FRIDAY) {
                                Text(
                                    text = day.narrowName(locale),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                weeks.forEach { week ->
                    Column(verticalArrangement = Arrangement.spacedBy(CellGap)) {
                        week.forEach { day -> CalendarCell(day, Modifier.size(cellSize)) }
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(CellGap, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.home_calendar_less),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            for (count in 0..2) {
                Box(Modifier.size(LegendCellSize).background(cellColor(count), CellShape))
            }
            Text(
                stringResource(R.string.home_calendar_more),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CalendarCell(day: CalendarDayUi, modifier: Modifier) {
    val cell = when {
        day.isFuture -> modifier
        day.isToday -> modifier
            .background(cellColor(day.workoutCount), CellShape)
            .border(TodayBorderWidth, MaterialTheme.colorScheme.primary, CellShape)
        else -> modifier.background(cellColor(day.workoutCount), CellShape)
    }
    Box(cell)
}

/** No workout, one, or more than one. */
@Composable
private fun cellColor(workoutCount: Int): Color = when {
    workoutCount <= 0 -> MaterialTheme.colorScheme.surfaceContainerHighest
    workoutCount == 1 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
    else -> MaterialTheme.colorScheme.primary
}
