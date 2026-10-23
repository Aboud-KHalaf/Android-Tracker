package com.example.tracker.ui.stats.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
 * One of the user's stats: the latest value, the change since the first, progress to the
 * target, and a log button. A long press offers Edit and Delete.
 */
@Composable
fun CustomStatCard(
    card: StatCardUi,
    today: LocalDate,
    onClick: () -> Unit,
    onLog: () -> Unit,
    modifier: Modifier = Modifier,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
) {
    val stat = card.stat
    val latest = card.latest
    var showMenu by remember { mutableStateOf(false) }
    Box(modifier) {
        StatCardContent(card, today, onClick, onLog, onLongClick = { showMenu = true })
        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.stat_edit_action)) },
                leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                onClick = {
                    showMenu = false
                    onEdit()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.stat_delete)) },
                leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                onClick = {
                    showMenu = false
                    onDelete()
                },
            )
        }
    }
}

@Composable
private fun StatCardContent(
    card: StatCardUi,
    today: LocalDate,
    onClick: () -> Unit,
    onLog: () -> Unit,
    onLongClick: () -> Unit,
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
        onLongClick = onLongClick,
        onLongClickLabel = stringResource(R.string.stat_more_actions),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun dayText(date: LocalDate, today: LocalDate): String =
    if (date == today) stringResource(R.string.stat_today) else date.longDate(currentLocale())

private const val SEPARATOR = " · "
