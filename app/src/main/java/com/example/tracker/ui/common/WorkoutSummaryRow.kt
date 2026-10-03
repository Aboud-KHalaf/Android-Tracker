package com.example.tracker.ui.common

import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.example.tracker.R
import com.example.tracker.domain.model.WorkoutSummary
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId

/** A finished workout as listed on Home and in History. */
data class WorkoutSummaryUi(
    val id: String,
    val name: String,
    val date: LocalDate,
    val exerciseCount: Int,
    val duration: Duration,
    val personalBestCount: Int,
)

fun WorkoutSummary.toWorkoutSummaryUi(zone: ZoneId) = WorkoutSummaryUi(
    id = id,
    name = name,
    date = startedAt.atZone(zone).toLocalDate(),
    exerciseCount = exerciseCount,
    duration = duration,
    personalBestCount = personalBestCount,
)

/**
 * Date tile, name, "Fri · 6 exercises · 55 min" and a PR badge. Transparent, so callers
 * choose the container (a grouped card on Home, plain in History).
 */
@Composable
fun WorkoutSummaryRow(workout: WorkoutSummaryUi, modifier: Modifier = Modifier) {
    val separator = stringResource(R.string.separator_dot)
    ListItem(
        modifier = modifier,
        headlineContent = { Text(workout.name) },
        supportingContent = {
            Text(
                workout.date.dayOfWeek.shortName(currentLocale()) + separator +
                    pluralStringResource(R.plurals.exercise_count, workout.exerciseCount, workout.exerciseCount) +
                    separator + formatDuration(workout.duration),
            )
        },
        leadingContent = { DateTile(workout.date) },
        trailingContent = if (workout.personalBestCount > 0) {
            {
                PersonalRecordBadge(
                    pluralStringResource(R.plurals.personal_record_count, workout.personalBestCount, workout.personalBestCount),
                )
            }
        } else {
            null
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}
