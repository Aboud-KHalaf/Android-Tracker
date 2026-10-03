package com.example.tracker.ui.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.example.tracker.ui.theme.spacing
import com.example.tracker.ui.theme.tabularNumbers
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/** "October 2026 ........ 3 workouts": a month in a list, with how many items it has. */
@Composable
fun MonthHeader(month: YearMonth, countText: String, modifier: Modifier = Modifier) {
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
            text = countText,
            style = MaterialTheme.typography.titleSmall.tabularNumbers().copy(fontWeight = FontWeight.Normal),
            color = color,
        )
    }
}
