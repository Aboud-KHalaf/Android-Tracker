package com.example.tracker.ui.exercises.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.example.tracker.ui.common.label
import com.example.tracker.ui.exercises.MuscleSetsUi
import com.example.tracker.ui.theme.spacing
import com.example.tracker.ui.theme.tabularNumbers

private val LabelWidth = 88.dp
private val BarHeight = 8.dp
private val CountWidth = 28.dp

/** "Sets this week by muscle": a bar per muscle group, with last week's count beside it. */
@Composable
fun MuscleWeekCard(rows: List<MuscleSetsUi>, modifier: Modifier = Modifier) {
    val most = rows.maxOf { maxOf(it.thisWeek, it.lastWeek) }.coerceAtLeast(1)
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
        ) {
            Text(
                stringResource(R.string.exercises_muscle_week_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            rows.forEach { row -> MuscleRow(row, most) }
            if (rows.any { it.group == null }) {
                Text(
                    stringResource(R.string.exercises_muscle_week_unassigned),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MuscleRow(row: MuscleSetsUi, most: Int) {
    val name = row.group.label()
    val description = stringResource(
        R.string.exercises_muscle_week_description,
        name,
        pluralStringResource(R.plurals.set_count_plain, row.thisWeek, row.thisWeek),
        pluralStringResource(R.plurals.set_count_plain, row.lastWeek, row.lastWeek),
    )
    Row(
        modifier = Modifier.clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
    ) {
        Text(name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(LabelWidth), maxLines = 1)
        Box(
            Modifier
                .weight(1f)
                .height(BarHeight)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape),
        ) {
            if (row.thisWeek > 0) {
                Box(
                    Modifier
                        .fillMaxWidth(row.thisWeek.toFloat() / most)
                        .height(BarHeight)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                )
            }
        }
        Text(
            row.thisWeek.toString(),
            style = MaterialTheme.typography.titleSmall.tabularNumbers(),
            modifier = Modifier.width(CountWidth),
        )
        Text(
            stringResource(R.string.exercises_muscle_week_last, row.lastWeek),
            style = MaterialTheme.typography.labelSmall.tabularNumbers(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
