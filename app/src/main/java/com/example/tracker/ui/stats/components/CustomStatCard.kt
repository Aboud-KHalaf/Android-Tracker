package com.example.tracker.ui.stats.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import com.example.tracker.R
import com.example.tracker.ui.common.currentLocale
import com.example.tracker.ui.common.longDate
import com.example.tracker.ui.stats.StatCardUi
import com.example.tracker.ui.stats.imageVector
import com.example.tracker.ui.stats.statChangeText
import com.example.tracker.ui.stats.statValueText
import com.example.tracker.ui.theme.statColors
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * What a stat card's menu offers; a null move means the stat is already at that end. The menu
 * opens on a long press released without dragging, and screen readers get its items as actions.
 */
data class StatCardMenu(
    val expanded: Boolean,
    val onDismiss: () -> Unit,
    val onEdit: () -> Unit,
    val onDelete: () -> Unit,
    val onMoveUp: (() -> Unit)?,
    val onMoveDown: (() -> Unit)?,
)

/**
 * One of the user's stats: the latest value, the change since the first, progress to the
 * target, and a log button, with [menu] below it.
 */
@Composable
fun CustomStatCard(
    card: StatCardUi,
    today: LocalDate,
    onClick: () -> Unit,
    onLog: () -> Unit,
    menu: StatCardMenu,
    modifier: Modifier = Modifier,
) {
    val items = menuItems(menu)
    Box(modifier) {
        StatCardContent(
            card = card,
            today = today,
            onClick = onClick,
            onLog = onLog,
            accessibilityActions = items.map { item ->
                CustomAccessibilityAction(item.label) {
                    item.onClick()
                    true
                }
            },
        )
        DropdownMenu(expanded = menu.expanded, onDismissRequest = menu.onDismiss) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item.label) },
                    leadingIcon = { Icon(item.icon, contentDescription = null) },
                    onClick = {
                        menu.onDismiss()
                        item.onClick()
                    },
                )
            }
        }
    }
}

private class MenuItem(val label: String, val icon: ImageVector, val onClick: () -> Unit)

@Composable
private fun menuItems(menu: StatCardMenu): List<MenuItem> = listOfNotNull(
    MenuItem(stringResource(R.string.stat_edit_action), Icons.Outlined.Edit, menu.onEdit),
    menu.onMoveUp?.let { MenuItem(stringResource(R.string.stat_move_up), Icons.Outlined.ArrowUpward, it) },
    menu.onMoveDown?.let { MenuItem(stringResource(R.string.stat_move_down), Icons.Outlined.ArrowDownward, it) },
    MenuItem(stringResource(R.string.stat_delete), Icons.Outlined.Delete, menu.onDelete),
)

@Composable
private fun StatCardContent(
    card: StatCardUi,
    today: LocalDate,
    onClick: () -> Unit,
    onLog: () -> Unit,
    accessibilityActions: List<CustomAccessibilityAction>,
) {
    val stat = card.stat
    val latest = card.latest
    StatCard(
        title = stat.name,
        icon = stat.icon.imageVector,
        accent = MaterialTheme.statColors[stat.color],
        value = latest?.let { statValueText(it.value, stat) },
        supporting = if (latest == null) {
            stringResource(R.string.stats_not_logged)
        } else {
            listOfNotNull(
                dayText(latest.date, today),
                card.changeSinceStart?.let { stringResource(R.string.stat_since_start, statChangeText(it, stat)) },
            ).joinToString(SEPARATOR)
        },
        progress = card.progress,
        progressLabel = card.progress?.let { progress ->
            stat.target?.let { stringResource(R.string.stat_target_progress, (progress * 100).roundToInt(), statValueText(it, stat)) }
        },
        trailing = {
            IconButton(onClick = onLog) {
                Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.stat_log_named, stat.name))
            }
        },
        onClick = onClick,
        accessibilityActions = accessibilityActions,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun dayText(date: LocalDate, today: LocalDate): String =
    if (date == today) stringResource(R.string.stat_today) else date.longDate(currentLocale())

private const val SEPARATOR = " · "
