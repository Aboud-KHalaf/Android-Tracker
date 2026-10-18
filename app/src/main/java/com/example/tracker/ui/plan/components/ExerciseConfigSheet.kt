package com.example.tracker.ui.plan.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.tracker.R
import com.example.tracker.domain.model.ExerciseConfig
import com.example.tracker.domain.model.ExerciseType
import com.example.tracker.ui.common.compactText
import com.example.tracker.ui.plan.ExerciseConfigForm
import com.example.tracker.ui.plan.ExerciseEditorUi
import com.example.tracker.ui.theme.Dimens
import com.example.tracker.ui.theme.spacing
import com.example.tracker.ui.theme.tabularNumbers

private val SetsValueWidth = 48.dp

/**
 * Edits one exercise's targets: sets, and reps and weight or hold time. Below them, set apart,
 * is what was actually recorded the last time, which these targets never change.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseConfigSheet(
    editor: ExerciseEditorUi,
    onSave: (ExerciseConfig) -> Unit,
    onDismiss: () -> Unit,
) {
    val initial = ExerciseConfigForm.from(editor.config)
    var sets by rememberSaveable(editor.planExerciseId) { mutableIntStateOf(initial.sets) }
    var reps by rememberSaveable(editor.planExerciseId) { mutableStateOf(initial.repsText) }
    var weight by rememberSaveable(editor.planExerciseId) { mutableStateOf(initial.weightText) }
    var hold by rememberSaveable(editor.planExerciseId) { mutableStateOf(initial.holdText) }
    val form = ExerciseConfigForm(sets, reps, weight, hold)
    val config = form.toConfig(editor.type)
    val spacing = MaterialTheme.spacing

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = spacing.lg, end = spacing.lg, bottom = spacing.xl),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                Text(
                    text = editor.name,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = stringResource(R.string.config_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SetsStepper(sets = form.sets, onChange = { sets = form.withSets(it).sets })
            when (editor.type) {
                ExerciseType.WEIGHT_REPS -> Row(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                    TargetField(
                        label = stringResource(R.string.config_reps),
                        value = reps,
                        onValueChange = { reps = it },
                        isValid = form.isRepsValid,
                        errorText = stringResource(R.string.config_invalid),
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f),
                    )
                    TargetField(
                        label = stringResource(R.string.config_weight),
                        value = weight,
                        onValueChange = { weight = it },
                        isValid = form.isWeightValid,
                        errorText = stringResource(R.string.config_invalid_weight),
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f),
                    )
                }

                ExerciseType.DURATION -> TargetField(
                    label = stringResource(R.string.config_hold),
                    value = hold,
                    onValueChange = { hold = it },
                    isValid = form.isHoldValid,
                    errorText = stringResource(R.string.config_invalid),
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            RecordedLastTime(editor)
            Button(
                onClick = { config?.let(onSave) },
                enabled = config != null && !editor.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = Dimens.minTouchTarget),
            ) {
                if (editor.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.config_save))
                }
            }
        }
    }
}

@Composable
private fun SetsStepper(sets: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.config_sets),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        FilledTonalIconButton(onClick = { onChange(sets - 1) }, enabled = sets > 1) {
            Icon(Icons.Outlined.Remove, contentDescription = stringResource(R.string.config_decrease_sets))
        }
        Text(
            text = sets.toString(),
            style = MaterialTheme.typography.titleLarge.tabularNumbers(),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .widthIn(min = SetsValueWidth)
                .semantics { liveRegion = LiveRegionMode.Polite },
        )
        FilledTonalIconButton(onClick = { onChange(sets + 1) }, enabled = sets < ExerciseConfig.MAX_TARGET_SETS) {
            Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.config_increase_sets))
        }
    }
}

@Composable
private fun TargetField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isValid: Boolean,
    errorText: String,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        supportingText = { Text(if (isValid) stringResource(R.string.config_repeat_last_time) else errorText) },
        isError = !isValid,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Done),
        modifier = modifier,
    )
}

/** What was really done last time: shown for comparison, never edited here. */
@Composable
private fun RecordedLastTime(editor: ExerciseEditorUi) {
    val spacing = MaterialTheme.spacing
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(spacing.md), horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
            Icon(Icons.Outlined.History, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                Text(stringResource(R.string.config_recorded), style = MaterialTheme.typography.labelLarge)
                Text(
                    text = editor.lastRecorded.takeIf { it.isNotEmpty() }?.compactText()
                        ?: stringResource(R.string.config_recorded_none),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
