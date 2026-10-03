package com.example.tracker.ui.history.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.example.tracker.R
import com.example.tracker.ui.common.currentLocale
import com.example.tracker.ui.theme.spacing
import com.example.tracker.ui.theme.tabularNumbers
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/** "October 2026 ........ 3 workouts". */
@Composable
fun MonthHeader(month: YearMonth, workoutCount: Int, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = modifier
            .padding(
                start = MaterialTheme.spacing.lg,
                end = MaterialTheme.spacing.lg,
                top = MaterialTheme.spacing.lg,
                bottom = MaterialTheme.spacing.sm,
            )
            .semantics(mergeDescendants = true) { heading() },
    ) {
        Text(
            text = month.format(DateTimeFormatter.ofPattern("LLLL yyyy", currentLocale())),
            style = MaterialTheme.typography.titleSmall,
            color = color,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = pluralStringResource(R.plurals.workout_count, workoutCount, workoutCount),
            style = MaterialTheme.typography.titleSmall.tabularNumbers().copy(fontWeight = FontWeight.Normal),
            color = color,
        )
    }
}
