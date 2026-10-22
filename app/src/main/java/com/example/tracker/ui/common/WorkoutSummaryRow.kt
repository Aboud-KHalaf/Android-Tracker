package com.example.tracker.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
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
    val note: String? = null,
)

fun WorkoutSummary.toWorkoutSummaryUi(zone: ZoneId) = WorkoutSummaryUi(
    id = id,
    name = name,
    date = startedAt.atZone(zone).toLocalDate(),
    exerciseCount = exerciseCount,
    duration = duration,
    personalBestCount = personalBestCount,
    note = note,
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
            Column {
                Text(
                    workout.date.dayOfWeek.shortName(currentLocale()) + separator +
                        pluralStringResource(R.plurals.exercise_count, workout.exerciseCount, workout.exerciseCount) +
                        separator + formatDuration(workout.duration),
                )
                workout.note?.let { NoteText(it) }
            }
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

/** A note under a list item: italic, at most two lines. */
@Composable
fun NoteText(note: String, modifier: Modifier = Modifier) {
    Text(
        text = note,
        fontStyle = FontStyle.Italic,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}
