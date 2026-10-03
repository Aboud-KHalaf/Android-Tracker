package com.example.tracker.ui.home.components

import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.example.tracker.R
import com.example.tracker.ui.common.DateTile
import com.example.tracker.ui.common.PersonalRecordBadge
import com.example.tracker.ui.common.currentLocale
import com.example.tracker.ui.common.formatDuration
import com.example.tracker.ui.common.shortName
import com.example.tracker.ui.home.RecentWorkoutUi

/** One finished workout in the grouped "Recent workouts" list. */
@Composable
fun RecentWorkoutRow(
    workout: RecentWorkoutUi,
    shape: Shape,
    modifier: Modifier = Modifier,
) {
    val separator = stringResource(R.string.separator_dot)
    Surface(shape = shape, color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = modifier) {
        ListItem(
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
                        pluralStringResource(
                            R.plurals.personal_record_count,
                            workout.personalBestCount,
                            workout.personalBestCount,
                        ),
                    )
                }
            } else {
                null
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )
    }
}
