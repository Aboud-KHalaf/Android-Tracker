package com.example.tracker.ui.program.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.tracker.R
import com.example.tracker.ui.common.IconAvatar
import com.example.tracker.ui.common.currentLocale
import com.example.tracker.ui.common.fullName
import com.example.tracker.ui.common.shortName
import com.example.tracker.ui.program.ProgramDayUi

private val WeekdayBadgeSize = 40.dp

/** One training day of the program; opens its exercise list, with a menu to move or remove it. */
@Composable
fun ProgramDayRow(
    day: ProgramDayUi,
    shape: Shape,
    onClick: () -> Unit,
    onChangeWeekday: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = currentLocale()
    val whenText = day.dayOfWeek?.fullName(locale) ?: stringResource(R.string.program_any_day)
    Surface(onClick = onClick, shape = shape, color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = modifier) {
        ListItem(
            headlineContent = { Text(day.name) },
            supportingContent = {
                Text(
                    whenText + stringResource(R.string.separator_dot) +
                        pluralStringResource(R.plurals.exercise_count, day.exerciseCount, day.exerciseCount),
                )
            },
            leadingContent = {
                val weekday = day.dayOfWeek
                if (weekday == null) {
                    IconAvatar(
                        icon = Icons.Outlined.EventRepeat,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                } else {
                    WeekdayBadge(weekday.shortName(locale))
                }
            },
            trailingContent = { DayOptionsMenu(day.name, onChangeWeekday = onChangeWeekday, onRemove = onRemove) },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )
    }
}

@Composable
private fun WeekdayBadge(text: String) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.size(WeekdayBadgeSize),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun DayOptionsMenu(dayName: String, onChangeWeekday: () -> Unit, onRemove: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Outlined.MoreVert, contentDescription = stringResource(R.string.program_day_options, dayName))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.program_change_weekday)) },
                leadingIcon = { Icon(Icons.Outlined.EditCalendar, contentDescription = null) },
                onClick = {
                    expanded = false
                    onChangeWeekday()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.program_remove_day)) },
                leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                colors = MenuDefaults.itemColors(
                    textColor = MaterialTheme.colorScheme.error,
                    leadingIconColor = MaterialTheme.colorScheme.error,
                ),
                onClick = {
                    expanded = false
                    onRemove()
                },
            )
        }
    }
}
