package com.example.tracker.ui.photos.measurements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.example.tracker.R
import com.example.tracker.domain.model.PhotoMeasurement
import com.example.tracker.ui.theme.Dimens
import com.example.tracker.ui.theme.spacing

/** What the user can do while entering measurements. */
data class MeasurementsActions(
    val onToggle: (name: String, unit: String) -> Unit = { _, _ -> },
    val onAdd: (name: String, unit: String) -> Unit = { _, _ -> },
    val onRemove: (index: Int) -> Unit = {},
    val onValueChange: (index: Int, text: String) -> Unit = { _, _ -> },
    val onSave: () -> Unit = {},
    /** Skips (after taking photos) or cancels (when editing). */
    val onDismiss: () -> Unit = {},
)

/**
 * Full-screen, optional measurements for a check-in: one-tap suggestions and custom ones,
 * each with a field for its value. [dismissLabel] names the top bar's way out, e.g. "Skip".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeasurementsScreen(
    form: MeasurementsForm,
    dismissLabel: String,
    actions: MeasurementsActions,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    var addingCustom by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.measurements_title)) },
                navigationIcon = {
                    IconButton(onClick = actions.onDismiss) {
                        Icon(Icons.Outlined.Close, contentDescription = dismissLabel)
                    }
                },
                actions = { TextButton(onClick = actions.onDismiss) { Text(dismissLabel) } },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Box(Modifier.fillMaxWidth().padding(MaterialTheme.spacing.lg), contentAlignment = Alignment.Center) {
                Button(
                    onClick = actions.onSave,
                    enabled = !form.isSaving,
                    modifier = Modifier
                        .widthIn(max = Dimens.maxContentWidth)
                        .fillMaxWidth(),
                ) { Text(stringResource(R.string.action_save)) }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding(),
            contentAlignment = Alignment.TopCenter,
        ) {
            MeasurementsContent(form, actions, onAddCustom = { addingCustom = true })
        }
    }

    if (addingCustom) {
        CustomMeasurementDialog(
            onAdd = { name, unit ->
                addingCustom = false
                actions.onAdd(name, unit)
            },
            onDismiss = { addingCustom = false },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MeasurementsContent(form: MeasurementsForm, actions: MeasurementsActions, onAddCustom: () -> Unit) {
    val spacing = MaterialTheme.spacing
    Column(
        modifier = Modifier
            .widthIn(max = Dimens.maxContentWidth)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.lg, vertical = spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        Text(
            text = stringResource(R.string.measurements_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            Text(
                text = stringResource(R.string.measurements_pick),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.semantics { heading() },
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                MeasurementSuggestion.entries.forEach { suggestion ->
                    val name = stringResource(suggestion.nameRes)
                    val unit = stringResource(suggestion.unitRes)
                    val selected = form.has(name)
                    FilterChip(
                        selected = selected,
                        onClick = { actions.onToggle(name, unit) },
                        label = { Text(name) },
                        leadingIcon = if (selected) {
                            { Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
                        } else {
                            null
                        },
                    )
                }
                AssistChip(
                    onClick = onAddCustom,
                    label = { Text(stringResource(R.string.measurements_custom)) },
                    leadingIcon = {
                        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(AssistChipDefaults.IconSize))
                    },
                )
            }
        }
        form.fields.forEachIndexed { index, field ->
            OutlinedTextField(
                value = field.value,
                onValueChange = { actions.onValueChange(index, it) },
                label = { Text(field.name) },
                suffix = field.unit.takeIf { it.isNotBlank() }?.let { unit -> { Text(unit) } },
                isError = field.error,
                supportingText = if (field.error) {
                    { Text(stringResource(R.string.stat_error_value)) }
                } else {
                    null
                },
                trailingIcon = {
                    IconButton(onClick = { actions.onRemove(index) }) {
                        Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.measurements_remove, field.name))
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = if (index == form.fields.lastIndex) ImeAction.Done else ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Asks for a measurement's name and, optionally, its unit. */
@Composable
private fun CustomMeasurementDialog(onAdd: (name: String, unit: String) -> Unit, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var unit by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.measurements_custom)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(PhotoMeasurement.MAX_NAME_LENGTH) },
                    label = { Text(stringResource(R.string.stat_name)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it.take(PhotoMeasurement.MAX_UNIT_LENGTH) },
                    label = { Text(stringResource(R.string.stat_unit)) },
                    placeholder = { Text(stringResource(R.string.stat_unit_placeholder)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onAdd(name, unit) }, enabled = name.isNotBlank()) { Text(stringResource(R.string.action_add)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
