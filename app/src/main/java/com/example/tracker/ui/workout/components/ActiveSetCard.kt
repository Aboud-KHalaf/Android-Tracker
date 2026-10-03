package com.example.tracker.ui.workout.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.tracker.R
import com.example.tracker.ui.common.currentLocale
import com.example.tracker.ui.common.formatWeight
import com.example.tracker.ui.common.text
import com.example.tracker.ui.theme.Dimens
import com.example.tracker.ui.theme.spacing
import com.example.tracker.ui.theme.tabularNumbers
import com.example.tracker.ui.workout.SetEditorUi
import com.example.tracker.ui.workout.SetInput
import com.example.tracker.ui.workout.SetRowUi

private val ActiveBorderWidth = 2.dp

/** Callbacks for editing the active weight × reps set. */
data class WeightRepsEditorActions(
    val onWeightChange: (String) -> Unit = {},
    val onRepsChange: (String) -> Unit = {},
    val onWeightStep: (steps: Int) -> Unit = {},
    val onRepsStep: (steps: Int) -> Unit = {},
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
            ActiveSetHeader(set, onRemove)
            Column(
                modifier = Modifier.padding(end = MaterialTheme.spacing.sm),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
            ) {
                when (val editor = set.editor) {
                    is SetEditorUi.WeightReps -> WeightRepsEditor(editor, weightRepsActions, onComplete)
                    is SetEditorUi.Duration -> DurationEditor(editor, durationActions, onComplete)
                }
            }
        }
    }
}

@Composable
private fun ActiveSetHeader(set: SetRowUi.Active, onRemove: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.workout_set_number, set.number), style = MaterialTheme.typography.titleMedium)
            Text(
                text = set.lastTime?.let { stringResource(R.string.workout_last_time_set, it.text()) }
                    ?: stringResource(R.string.workout_new_set),
                style = MaterialTheme.typography.bodyMedium.tabularNumbers(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onRemove) {
            Icon(
                Icons.Outlined.Delete,
                contentDescription = stringResource(R.string.workout_remove_set, set.number),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WeightRepsEditor(
    editor: SetEditorUi.WeightReps,
    actions: WeightRepsEditorActions,
    onComplete: () -> Unit,
) {
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
    )
    Button(
        onClick = onComplete,
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
