package com.example.tracker.ui.nutrition.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.example.tracker.R
import com.example.tracker.domain.model.DailyNutrition
import com.example.tracker.ui.common.currentLocale
import com.example.tracker.ui.nutrition.DayEditorState
import com.example.tracker.ui.nutrition.formatAmount
import com.example.tracker.ui.nutrition.toLocalDate
import com.example.tracker.ui.nutrition.toUtcMillis
import com.example.tracker.ui.common.longDate
import com.example.tracker.ui.theme.spacing
import java.time.LocalDate

/** Logs or edits one day: its date, calories and protein. */
@Composable
fun DayEditorDialog(
    state: DayEditorState,
    today: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    onCaloriesChange: (String) -> Unit,
    onProteinChange: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    val locale = currentLocale()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (state.isExisting) R.string.nutrition_edit_day else R.string.nutrition_log_day)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)) {
                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.CalendarMonth, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text(
                        text = if (state.date == today) stringResource(R.string.nutrition_today) else state.date.longDate(locale),
                        modifier = Modifier.weight(1f),
                    )
                }
                AmountField(
                    value = state.calories,
                    onValueChange = onCaloriesChange,
                    label = stringResource(R.string.nutrition_calories),
                    suffix = stringResource(R.string.nutrition_unit_kcal),
                    isError = state.caloriesError,
                    errorText = stringResource(R.string.nutrition_error_amount, formatAmount(DailyNutrition.MAX_CALORIES, locale)),
                    imeAction = ImeAction.Next,
                )
                AmountField(
                    value = state.protein,
                    onValueChange = onProteinChange,
                    label = stringResource(R.string.nutrition_protein),
                    suffix = stringResource(R.string.nutrition_unit_grams),
                    isError = state.proteinError,
                    errorText = stringResource(R.string.nutrition_error_amount, formatAmount(DailyNutrition.MAX_PROTEIN_GRAMS, locale)),
                    imeAction = ImeAction.Done,
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

/** A whole-number field with a unit suffix and an error message below. */
@Composable
internal fun AmountField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    suffix: String,
    isError: Boolean,
    errorText: String,
    imeAction: ImeAction,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        suffix = { Text(suffix) },
        isError = isError,
        supportingText = if (isError) {
            { Text(errorText) }
        } else {
            null
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = imeAction),
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Picks one day, up to [latest]: nothing can be logged for days that haven't happened. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayPickerDialog(
    selected: LocalDate,
    latest: LocalDate,
    onPick: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val latestMillis = latest.toUtcMillis()
    val selectableDates = remember(latestMillis) {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= latestMillis
            override fun isSelectableYear(year: Int) = year <= latest.year
        }
    }
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = selected.toUtcMillis(),
        selectableDates = selectableDates,
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { pickerState.selectedDateMillis?.let { onPick(it.toLocalDate()) } },
                enabled = pickerState.selectedDateMillis != null,
            ) { Text(stringResource(R.string.action_ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    ) {
        DatePicker(state = pickerState)
    }
}
