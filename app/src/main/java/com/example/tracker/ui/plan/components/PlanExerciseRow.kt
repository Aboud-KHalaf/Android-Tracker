package com.example.tracker.ui.plan.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.example.tracker.R
import com.example.tracker.ui.common.IconAvatar
import com.example.tracker.ui.common.icon
import com.example.tracker.ui.common.label
import com.example.tracker.ui.plan.PlanExerciseUi

/** One exercise in the plan, with a menu to reorder or remove it. */
@Composable
fun PlanExerciseRow(
    exercise: PlanExerciseUi,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        modifier = modifier,
        headlineContent = { Text(exercise.name) },
        supportingContent = {
            Text(
                pluralStringResource(R.plurals.set_count, exercise.targetSets, exercise.targetSets) +
                    stringResource(R.string.separator_dot) + exercise.type.label(),
            )
        },
        leadingContent = {
            IconAvatar(
                icon = exercise.type.icon,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        },
        trailingContent = {
            ExerciseOptionsMenu(exercise, onMoveUp = onMoveUp, onMoveDown = onMoveDown, onRemove = onRemove)
        },
    )
}

@Composable
private fun ExerciseOptionsMenu(
    exercise: PlanExerciseUi,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    fun select(action: () -> Unit) {
        expanded = false
        action()
    }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                Icons.Outlined.MoreVert,
                contentDescription = stringResource(R.string.plan_exercise_options, exercise.name),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.plan_move_up)) },
                leadingIcon = { Icon(Icons.Outlined.ArrowUpward, contentDescription = null) },
                enabled = exercise.canMoveUp,
                onClick = { select(onMoveUp) },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.plan_move_down)) },
                leadingIcon = { Icon(Icons.Outlined.ArrowDownward, contentDescription = null) },
                enabled = exercise.canMoveDown,
                onClick = { select(onMoveDown) },
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(stringResource(R.string.plan_remove)) },
                leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                colors = MenuDefaults.itemColors(
                    textColor = MaterialTheme.colorScheme.error,
                    leadingIconColor = MaterialTheme.colorScheme.error,
                ),
                onClick = { select(onRemove) },
            )
        }
    }
}
