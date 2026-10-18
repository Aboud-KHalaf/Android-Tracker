package com.example.tracker.ui.program.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.example.tracker.R
import com.example.tracker.ui.common.currentLocale
import com.example.tracker.ui.common.fullName
import com.example.tracker.ui.program.ProgramDayUi
import com.example.tracker.ui.theme.Dimens
import com.example.tracker.ui.theme.spacing
import java.time.DayOfWeek

/**
 * Moves [day] to another weekday or to "no fixed weekday". Weekdays another day already
 * uses are shown with that day's name and can't be picked. Picking an option saves it.
 */
@Composable
fun ScheduleDayDialog(
    day: ProgramDayUi,
    allDays: List<ProgramDayUi>,
    onSchedule: (DayOfWeek?) -> Unit,
    onDismiss: () -> Unit,
) {
    val locale = currentLocale()
    val takenBy = allDays.filter { it.id != day.id && it.dayOfWeek != null }.associateBy { it.dayOfWeek }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.program_schedule_title, day.name)) },
        text = {
            Column(Modifier.selectableGroup().verticalScroll(rememberScrollState())) {
                DayOfWeek.entries.forEach { weekday ->
                    val other = takenBy[weekday]
                    ScheduleOption(
                        label = if (other == null) {
                            weekday.fullName(locale)
                        } else {
                            stringResource(R.string.program_schedule_taken, weekday.fullName(locale), other.name)
                        },
                        selected = day.dayOfWeek == weekday,
                        enabled = other == null,
                        onClick = { onSchedule(weekday) },
                    )
                }
                ScheduleOption(
                    label = stringResource(R.string.program_schedule_none),
                    selected = day.dayOfWeek == null,
                    enabled = true,
                    onClick = { onSchedule(null) },
                )
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
private fun ScheduleOption(label: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.minTouchTarget)
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null, enabled = enabled)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = DisabledAlpha),
            modifier = Modifier.padding(start = MaterialTheme.spacing.md),
        )
    }
}

/** Material's opacity for disabled content. */
private const val DisabledAlpha = 0.38f
