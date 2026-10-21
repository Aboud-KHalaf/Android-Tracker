package com.example.tracker.ui.plan.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FindReplace
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.SyncAlt
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.tracker.R
import com.example.tracker.domain.model.ExerciseConfig
import com.example.tracker.domain.model.ExerciseType
import com.example.tracker.ui.common.IconAvatar
import com.example.tracker.ui.common.currentLocale
import com.example.tracker.ui.common.formatClock
import com.example.tracker.ui.common.formatWeight
import com.example.tracker.ui.common.icon
import com.example.tracker.ui.common.label
import com.example.tracker.ui.plan.PlanExerciseUi

private val DraggedElevation = 6.dp

/** What [PlanExerciseRow]'s menu and gestures can do. */
data class PlanExerciseRowActions(
    val onEdit: () -> Unit,
    val onReplace: () -> Unit,
    val onMoveUp: () -> Unit,
    val onMoveDown: () -> Unit,
    val onRemove: () -> Unit,
    val onSupersetWithNext: () -> Unit = {},
    val onRemoveFromSuperset: () -> Unit = {},
)

/**
 * One exercise of the day with its targets. Tapping it edits the targets; it is reordered with
 * [dragHandle] (and long-pressing the row, see the caller's [modifier]), or with the menu and
 * accessibility actions.
 */
@Composable
fun PlanExerciseRow(
    exercise: PlanExerciseUi,
    isDragging: Boolean,
    actions: PlanExerciseRowActions,
    modifier: Modifier = Modifier,
    dragHandle: @Composable () -> Unit = {},
) {
    val moveUpLabel = stringResource(R.string.plan_move_up)
    val moveDownLabel = stringResource(R.string.plan_move_down)
    Surface(
        onClick = actions.onEdit,
        color = if (isDragging) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surface,
        shadowElevation = if (isDragging) DraggedElevation else 0.dp,
        modifier = modifier.semantics {
            customActions = buildList {
                if (exercise.canMoveUp) add(CustomAccessibilityAction(moveUpLabel) { actions.onMoveUp(); true })
                if (exercise.canMoveDown) add(CustomAccessibilityAction(moveDownLabel) { actions.onMoveDown(); true })
            }
        },
    ) {
        ListItem(
            headlineContent = { Text(exercise.name) },
            supportingContent = {
                val summary = exercise.config.summary(exercise.type)
                if (exercise.supersetLabel == null) {
                    Text(summary)
                } else {
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Medium)) {
                                append(stringResource(R.string.plan_superset_label, exercise.supersetLabel))
                            }
                            append(stringResource(R.string.separator_dot))
                            append(summary)
                        },
                    )
                }
            },
            leadingContent = {
                IconAvatar(
                    icon = exercise.type.icon,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            },
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ExerciseOptionsMenu(exercise, actions)
                    dragHandle()
                }
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )
    }
}

@Composable
private fun ExerciseOptionsMenu(exercise: PlanExerciseUi, actions: PlanExerciseRowActions) {
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
                text = { Text(stringResource(R.string.plan_edit_targets)) },
                leadingIcon = { Icon(Icons.Outlined.Tune, contentDescription = null) },
                onClick = { select(actions.onEdit) },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.plan_replace)) },
                leadingIcon = { Icon(Icons.Outlined.FindReplace, contentDescription = null) },
                onClick = { select(actions.onReplace) },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.plan_move_up)) },
                leadingIcon = { Icon(Icons.Outlined.ArrowUpward, contentDescription = null) },
                enabled = exercise.canMoveUp,
                onClick = { select(actions.onMoveUp) },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.plan_move_down)) },
                leadingIcon = { Icon(Icons.Outlined.ArrowDownward, contentDescription = null) },
                enabled = exercise.canMoveDown,
                onClick = { select(actions.onMoveDown) },
            )
            HorizontalDivider()
            if (exercise.canSupersetWithNext) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.plan_superset_with_next)) },
                    leadingIcon = { Icon(Icons.Outlined.SyncAlt, contentDescription = null) },
                    onClick = { select(actions.onSupersetWithNext) },
                )
            }
            if (exercise.supersetLabel != null) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.plan_remove_from_superset)) },
                    leadingIcon = { Icon(Icons.Outlined.LinkOff, contentDescription = null) },
                    onClick = { select(actions.onRemoveFromSuperset) },
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(stringResource(R.string.plan_remove)) },
                leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                colors = MenuDefaults.itemColors(
                    textColor = MaterialTheme.colorScheme.error,
                    leadingIconColor = MaterialTheme.colorScheme.error,
                ),
                onClick = { select(actions.onRemove) },
            )
        }
    }
}

/** "3 sets · 8 reps · 60 kg", "3 sets · 0:45 hold", or "3 sets · Weight × reps" without targets. */
@Composable
@ReadOnlyComposable
fun ExerciseConfig.summary(type: ExerciseType): String {
    val locale = currentLocale()
    val targets = when (type) {
        ExerciseType.WEIGHT_REPS -> listOfNotNull(
            targetReps?.let { pluralStringResource(R.plurals.target_reps, it, it) },
            targetWeightKg?.let { stringResource(R.string.target_weight, formatWeight(it, locale)) },
        )
        ExerciseType.DURATION -> listOfNotNull(
            targetDurationSeconds?.let { stringResource(R.string.target_hold, formatClock(it)) },
        )
    }
    val parts = listOf(pluralStringResource(R.plurals.set_count, targetSets, targetSets)) +
        targets.ifEmpty { listOf(type.label()) }
    return parts.joinToString(stringResource(R.string.separator_dot))
}
