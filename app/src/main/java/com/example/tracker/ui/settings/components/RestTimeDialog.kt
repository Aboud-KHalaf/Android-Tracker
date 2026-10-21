package com.example.tracker.ui.settings.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.example.tracker.R
import com.example.tracker.domain.workout.RestTimes
import com.example.tracker.ui.common.formatRestTime

/** Lets the user pick a rest time from [RestTimes.OPTIONS]; choosing one applies it right away. */
@Composable
fun RestTimeDialog(
    title: String,
    selectedSeconds: Int,
    onSelect: (seconds: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .selectableGroup()
                    .verticalScroll(rememberScrollState()),
            ) {
                RestTimes.OPTIONS.forEach { seconds ->
                    val isSelected = seconds == selectedSeconds
                    ListItem(
                        headlineContent = { Text(formatRestTime(seconds)) },
                        leadingContent = { RadioButton(selected = isSelected, onClick = null) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.selectable(
                            selected = isSelected,
                            onClick = { onSelect(seconds) },
                            role = Role.RadioButton,
                        ),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
