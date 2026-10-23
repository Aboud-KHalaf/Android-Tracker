package com.example.tracker.ui.workout.components

import androidx.compose.foundation.BorderStroke
import com.example.tracker.ui.common.SetValueUi
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material.icons.outlined.History
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AssistChip
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.tracker.R
import com.example.tracker.domain.model.Rpe
import com.example.tracker.domain.model.SetType
import com.example.tracker.ui.common.currentLocale
import com.example.tracker.ui.common.formatNumber
import com.example.tracker.ui.common.formatWeight
import com.example.tracker.ui.common.text
import com.example.tracker.ui.theme.Dimens
import com.example.tracker.ui.theme.spacing
import com.example.tracker.ui.theme.tabularNumbers
import com.example.tracker.ui.workout.SetEditorUi
import com.example.tracker.ui.workout.SetInput
import com.example.tracker.ui.workout.SetRowUi
import com.example.tracker.ui.workout.label
import com.example.tracker.ui.workout.setName

private val ActiveBorderWidth = 2.dp

/** Callbacks for editing the active weight × reps set. */
data class WeightRepsEditorActions(
    val onWeightChange: (String) -> Unit = {},
    val onRepsChange: (String) -> Unit = {},
    val onWeightStep: (steps: Int) -> Unit = {},
    val onRepsStep: (steps: Int) -> Unit = {},
    /** A rating, or null to clear it. */
    val onRpeChange: (Double?) -> Unit = {},
    /** Fills in the weight and reps of the same set last time. */
    val onUseLastTime: () -> Unit = {},
)

/** The set being edited: last time's values, its inputs, and Complete. */
@Composable
fun ActiveSetCard(
    set: SetRowUi.Active,
    weightRepsActions: WeightRepsEditorActions,
    durationActions: DurationEditorActions,
    onComplete: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    onSelectType: (SetType) -> Unit = {},
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(ActiveBorderWidth, MaterialTheme.colorScheme.primary),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(
                start = MaterialTheme.spacing.lg,
                end = MaterialTheme.spacing.sm,
                top = MaterialTheme.spacing.md,
                bottom = MaterialTheme.spacing.lg,
            ),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
        ) {
            ActiveSetHeader(set, onRemove, onSelectType)
            Column(
                modifier = Modifier.padding(end = MaterialTheme.spacing.sm),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
            ) {
                when (val editor = set.editor) {
                    is SetEditorUi.WeightReps -> WeightRepsEditor(
                        editor = editor,
                        lastTime = set.lastTime as? SetValueUi.WeightReps,
                        rpe = set.rpe,
                        actions = weightRepsActions,
                        onComplete = onComplete,
                    )
                    is SetEditorUi.Duration -> DurationEditor(editor, durationActions, onComplete)
                }
            }
        }
    }
}

@Composable
private fun ActiveSetHeader(set: SetRowUi.Active, onRemove: () -> Unit, onSelectType: (SetType) -> Unit) {
    val name = setName(set.type, set.number)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.titleMedium)
            Text(
                text = when {
                    set.type.isWarmUp -> stringResource(R.string.set_type_warm_up_description)
                    set.lastTime != null -> stringResource(R.string.workout_last_time_set, set.lastTime.text())
                    else -> stringResource(R.string.workout_new_set)
                },
                style = MaterialTheme.typography.bodyMedium.tabularNumbers(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SetTypeChip(selected = set.type, onSelect = onSelectType)
        IconButton(onClick = onRemove) {
            Icon(
                Icons.Outlined.Delete,
                contentDescription = stringResource(R.string.workout_remove_set, name),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Shows the active set's type; tapping offers the others. */
@Composable
private fun SetTypeChip(selected: SetType, onSelect: (SetType) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val description = stringResource(R.string.workout_set_type, selected.label())
    Box {
        AssistChip(
            onClick = { expanded = true },
            label = { Text(selected.label()) },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
            modifier = Modifier.semantics { contentDescription = description },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SetType.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.label()) },
                    leadingIcon = if (type == selected) {
                        { Icon(Icons.Outlined.Check, contentDescription = null) }
                    } else {
                        null
                    },
                    onClick = {
                        expanded = false
                        onSelect(type)
                    },
                )
            }
        }
    }
}

@Composable
private fun WeightRepsEditor(
    editor: SetEditorUi.WeightReps,
    lastTime: SetValueUi.WeightReps?,
    rpe: Double?,
    actions: WeightRepsEditorActions,
    onComplete: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val repsFocus = remember { FocusRequester() }
    var showPlates by rememberSaveable { mutableStateOf(false) }
    QuickActions(
        // Offered only while it would change something.
        lastTime = lastTime?.takeIf { it.weightKg != editor.weightKg || it.reps != editor.reps },
        onUseLastTime = {
            focusManager.clearFocus()
            actions.onUseLastTime()
        },
        onShowPlates = { showPlates = true },
    )
    if (showPlates) PlateCalculatorSheet(weightKg = editor.weightKg, onDismiss = { showPlates = false })
    val step = formatWeight(SetInput.WEIGHT_STEP_KG, currentLocale())
    StepperField(
        label = stringResource(R.string.workout_weight_label),
        value = editor.weightText,
        isValid = editor.isWeightValid,
        keyboardType = KeyboardType.Decimal,
        decreaseDescription = stringResource(R.string.workout_decrease_weight, step),
        increaseDescription = stringResource(R.string.workout_increase_weight, step),
        onValueChange = actions.onWeightChange,
        onDecrease = { actions.onWeightStep(-1) },
        onIncrease = { actions.onWeightStep(1) },
        // The keyboard's Next goes straight on to the reps.
        imeAction = ImeAction.Next,
        onImeAction = { repsFocus.requestFocus() },
    )
    StepperField(
        label = stringResource(R.string.workout_reps_label),
        value = editor.repsText,
        isValid = editor.isRepsValid,
        keyboardType = KeyboardType.Number,
        decreaseDescription = stringResource(R.string.workout_decrease_reps),
        increaseDescription = stringResource(R.string.workout_increase_reps),
        onValueChange = actions.onRepsChange,
        onDecrease = { actions.onRepsStep(-1) },
        onIncrease = { actions.onRepsStep(1) },
        focusRequester = repsFocus,
    )
    RpeSelector(selected = rpe, onSelect = actions.onRpeChange)
    Button(
        onClick = {
            focusManager.clearFocus()
            onComplete()
        },
        enabled = editor.canComplete,
        modifier = Modifier
            .fillMaxWidth()
            .height(Dimens.setControlSize),
    ) {
        Icon(Icons.Outlined.Check, contentDescription = null)
        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
        Text(stringResource(R.string.workout_complete_set), style = MaterialTheme.typography.titleMedium)
    }
}

/** "Same as last time" (when there is a last time to copy) and the plate calculator. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickActions(lastTime: SetValueUi.WeightReps?, onUseLastTime: () -> Unit, onShowPlates: () -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
        verticalArrangement = Arrangement.Center,
    ) {
        if (lastTime != null) {
            AssistChip(
                onClick = onUseLastTime,
                label = {
                    Text(
                        stringResource(R.string.workout_use_last_time, lastTime.text()),
                        style = MaterialTheme.typography.labelLarge.tabularNumbers(),
                    )
                },
                leadingIcon = {
                    Icon(Icons.Outlined.History, contentDescription = null, modifier = Modifier.size(AssistChipDefaults.IconSize))
                },
            )
        }
        AssistChip(
            onClick = onShowPlates,
            label = { Text(stringResource(R.string.plates_action)) },
            leadingIcon = {
                Icon(Icons.Outlined.FitnessCenter, contentDescription = null, modifier = Modifier.size(AssistChipDefaults.IconSize))
            },
        )
    }
}

/** Optional effort rating: tapping a value picks it, tapping it again clears it. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RpeSelector(selected: Double?, onSelect: (Double?) -> Unit) {
    val locale = currentLocale()
    Column {
        Text(stringResource(R.string.workout_rpe_label), style = MaterialTheme.typography.labelLarge)
        Text(
            text = stringResource(R.string.workout_rpe_help),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
            Rpe.choices.forEach { value ->
                val number = formatNumber(value, locale)
                val description = stringResource(R.string.workout_rpe_value, number)
                FilterChip(
                    selected = value == selected,
                    onClick = { onSelect(if (value == selected) null else value) },
                    label = { Text(number) },
                    modifier = Modifier.semantics { contentDescription = description },
                )
            }
        }
    }
}
