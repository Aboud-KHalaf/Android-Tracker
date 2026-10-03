package com.example.tracker.ui.workout.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.tracker.R
import com.example.tracker.ui.common.SetValueUi
import com.example.tracker.ui.common.text
import com.example.tracker.ui.theme.Dimens
import com.example.tracker.ui.theme.spacing
import com.example.tracker.ui.theme.tabularNumbers
import com.example.tracker.ui.workout.SetRowUi

/** Width of the "Set 1" label, so values line up across rows. */
private val SetLabelWidth = 48.dp
private val RowMinHeight = 56.dp

/** A completed set; tapping it reopens it for editing. */
@Composable
fun DoneSetRow(set: SetRowUi.Done, onEdit: () -> Unit, modifier: Modifier = Modifier) {
    val valueText = set.value.textOrNotSet()
    val editLabel = stringResource(R.string.workout_edit_set)
    val description = stringResource(R.string.workout_set_done_description, set.number, valueText)
    Surface(
        onClick = onEdit,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
                onClick(label = editLabel) { onEdit(); true }
            },
    ) {
        SetRowLayout {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            SetLabel(set.number, MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = valueText,
                style = MaterialTheme.typography.titleMedium.tabularNumbers(),
                modifier = Modifier.weight(1f),
            )
            set.improvement?.let { improvement ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.ArrowUpward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = improvement.text(),
                        style = MaterialTheme.typography.labelMedium.tabularNumbers(),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

/** A set not done yet; tapping it makes it the active set. */
@Composable
fun UpcomingSetRow(set: SetRowUi.Upcoming, onSelect: () -> Unit, modifier: Modifier = Modifier) {
    val valueText = set.planned.textOrNotSet()
    val selectLabel = stringResource(R.string.workout_go_to_set)
    val description = stringResource(R.string.workout_set_upcoming_description, set.number, valueText)
    Surface(
        onClick = onSelect,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
                onClick(label = selectLabel) { onSelect(); true }
            },
    ) {
        SetRowLayout {
            Icon(Icons.Outlined.RadioButtonUnchecked, contentDescription = null)
            SetLabel(set.number, MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = valueText,
                style = MaterialTheme.typography.bodyLarge.tabularNumbers(),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Shown under the sets once every set of the exercise is done. */
@Composable
fun AllSetsDoneBanner(modifier: Modifier = Modifier) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(MaterialTheme.spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
        ) {
            Icon(Icons.Filled.TaskAlt, contentDescription = null)
            Text(stringResource(R.string.workout_all_sets_done), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun SetRowLayout(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .heightIn(min = maxOf(RowMinHeight, Dimens.minTouchTarget))
            .padding(horizontal = MaterialTheme.spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
        content = content,
    )
}

@Composable
private fun SetLabel(number: Int, color: Color) {
    Text(
        text = stringResource(R.string.workout_set_number, number),
        style = MaterialTheme.typography.labelLarge,
        color = color,
        modifier = Modifier.widthIn(min = SetLabelWidth),
    )
}

@Composable
private fun SetValueUi?.textOrNotSet(): String = this?.text() ?: stringResource(R.string.workout_value_not_set)
