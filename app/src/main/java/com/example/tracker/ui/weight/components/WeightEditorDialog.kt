package com.example.tracker.ui.weight.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.example.tracker.R
import com.example.tracker.domain.model.WeightEntry
import com.example.tracker.ui.common.DayPickerDialog
import com.example.tracker.ui.common.currentLocale
import com.example.tracker.ui.common.formatWeight
import com.example.tracker.ui.common.longDate
import com.example.tracker.ui.theme.spacing
import com.example.tracker.ui.weight.WeightEditorState
import java.time.LocalDate

/** Logs or edits one day's weight. */
@Composable
fun WeightEditorDialog(
    state: WeightEditorState,
    today: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    onWeightChange: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    val locale = currentLocale()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (state.isExisting) R.string.weight_edit else R.string.weight_log)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)) {
                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.CalendarMonth, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text(
                        text = if (state.date == today) stringResource(R.string.weight_today) else state.date.longDate(locale),
                        modifier = Modifier.weight(1f),
                    )
                }
                OutlinedTextField(
                    value = state.weight,
                    onValueChange = onWeightChange,
                    label = { Text(stringResource(R.string.weight_title)) },
                    suffix = { Text(stringResource(R.string.weight_unit_kg)) },
                    isError = state.weightError,
                    supportingText = if (state.weightError) {
                        {
                            Text(
                                stringResource(
                                    R.string.weight_error_value,
                                    formatWeight(WeightEntry.MIN_KG, locale),
                                    formatWeight(WeightEntry.MAX_KG, locale),
                                ),
                            )
                        }
                    } else {
                        null
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onSave, enabled = !state.isSaving) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            Row {
                if (state.isExisting) {
                    TextButton(
                        onClick = onDelete,
                        enabled = !state.isSaving,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) { Text(stringResource(R.string.action_delete)) }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
            }
        },
    )

    if (showDatePicker) {
        DayPickerDialog(
            selected = state.date,
            latest = today,
            onPick = { date ->
                showDatePicker = false
                onDateChange(date)
            },
            onDismiss = { showDatePicker = false },
        )
    }
}
