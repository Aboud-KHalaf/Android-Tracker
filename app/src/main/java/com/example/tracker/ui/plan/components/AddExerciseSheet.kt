package com.example.tracker.ui.plan.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.example.tracker.R
import com.example.tracker.domain.model.ExerciseType
import com.example.tracker.domain.model.MuscleGroup
import com.example.tracker.ui.common.ExerciseTypeSelector
import com.example.tracker.ui.common.IconAvatar
import com.example.tracker.ui.common.MuscleGroupSelector
import com.example.tracker.ui.common.icon
import com.example.tracker.ui.common.label
import com.example.tracker.ui.plan.ExerciseOptionUi
import com.example.tracker.ui.theme.Dimens
import com.example.tracker.ui.theme.spacing

/**
 * Picks an exercise from the library or creates a new one. Used to add an exercise to the day
 * and, with its own [title], to replace one.
 * Every exercise added this way has a target muscle: a new one asks for it, and a library
 * exercise without one asks before it's added ([onAdd] then gets the choice, otherwise null).
 * Calls [onDismiss] after any of these.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExerciseSheet(
    options: List<ExerciseOptionUi>,
    onAdd: (exerciseId: String, muscleGroup: MuscleGroup?) -> Unit,
    onCreate: (name: String, type: ExerciseType, muscleGroup: MuscleGroup) -> Unit,
    onDismiss: () -> Unit,
    title: String = stringResource(R.string.add_exercise_title),
    emptyText: String = stringResource(R.string.add_exercise_library_empty),
    createLabel: String = stringResource(R.string.add_exercise_confirm),
) {
    var isCreating by rememberSaveable { mutableStateOf(false) }
    // A library exercise without a muscle group, waiting for one before it's added.
    var needsMuscleId by rememberSaveable { mutableStateOf<String?>(null) }
    val needsMuscle = needsMuscleId?.let { id -> options.firstOrNull { it.id == id } }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        when {
            isCreating -> NewExerciseForm(
                confirmLabel = createLabel,
                onCreate = { name, type, muscleGroup ->
                    onCreate(name, type, muscleGroup)
                    onDismiss()
                },
            )

            needsMuscle != null -> MuscleGroupForm(
                exerciseName = needsMuscle.name,
                confirmLabel = createLabel,
                onConfirm = { muscleGroup ->
                    onAdd(needsMuscle.id, muscleGroup)
                    onDismiss()
                },
            )

            else -> ExercisePicker(
                title = title,
                emptyText = emptyText,
                options = options,
                onPick = { option ->
                    if (option.muscleGroup == null) {
                        needsMuscleId = option.id
                    } else {
                        onAdd(option.id, null)
                        onDismiss()
                    }
                },
                onCreateNew = { isCreating = true },
            )
        }
    }
}

@Composable
private fun ExercisePicker(
    title: String,
    emptyText: String,
    options: List<ExerciseOptionUi>,
    onPick: (ExerciseOptionUi) -> Unit,
    onCreateNew: () -> Unit,
) {
    LazyColumn(modifier = Modifier.padding(bottom = MaterialTheme.spacing.xl)) {
        item(key = "title") {
            SheetTitle(
                text = title,
                modifier = Modifier.padding(horizontal = MaterialTheme.spacing.lg, vertical = MaterialTheme.spacing.sm),
            )
        }
        item(key = "create") {
            ListItem(
                headlineContent = { Text(stringResource(R.string.add_exercise_create)) },
                leadingContent = {
                    IconAvatar(
                        icon = Icons.Outlined.Add,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable(onClick = onCreateNew),
            )
            HorizontalDivider()
        }
        if (options.isEmpty()) {
            item(key = "empty") {
                Text(
                    text = emptyText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(MaterialTheme.spacing.lg),
                )
            }
        }
        items(options, key = { it.id }) { option ->
            ListItem(
                headlineContent = { Text(option.name) },
                supportingContent = {
                    Text(listOfNotNull(option.type.label(), option.muscleGroup?.label()).joinToString(" · "))
                },
                leadingContent = {
                    IconAvatar(
                        icon = option.type.icon,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable { onPick(option) },
            )
        }
    }
}

@Composable
private fun NewExerciseForm(confirmLabel: String, onCreate: (String, ExerciseType, MuscleGroup) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf(ExerciseType.WEIGHT_REPS) }
    var muscleGroup by rememberSaveable { mutableStateOf<MuscleGroup?>(null) }
    val spacing = MaterialTheme.spacing

    Column(
        modifier = Modifier.padding(start = spacing.lg, end = spacing.lg, bottom = spacing.xl),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        SheetTitle(stringResource(R.string.add_exercise_new_title))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(R.string.add_exercise_name_label)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Done,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        ExerciseTypeSelector(selected = type, onSelect = { type = it })
        MuscleGroupSelector(selected = muscleGroup, onSelect = { muscleGroup = it })
        val chosenMuscle = muscleGroup
        Button(
            onClick = { if (chosenMuscle != null) onCreate(name, type, chosenMuscle) },
            enabled = name.isNotBlank() && chosenMuscle != null,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Dimens.minTouchTarget),
        ) { Text(confirmLabel) }
    }
}

/** Asks which muscle a library exercise trains before it's added. */
@Composable
private fun MuscleGroupForm(exerciseName: String, confirmLabel: String, onConfirm: (MuscleGroup) -> Unit) {
    var muscleGroup by rememberSaveable { mutableStateOf<MuscleGroup?>(null) }
    val spacing = MaterialTheme.spacing

    Column(
        modifier = Modifier.padding(start = spacing.lg, end = spacing.lg, bottom = spacing.xl),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            SheetTitle(stringResource(R.string.add_exercise_muscle_title, exerciseName))
            Text(
                text = stringResource(R.string.add_exercise_muscle_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        MuscleGroupSelector(selected = muscleGroup, onSelect = { muscleGroup = it })
        val chosenMuscle = muscleGroup
        Button(
            onClick = { if (chosenMuscle != null) onConfirm(chosenMuscle) },
            enabled = chosenMuscle != null,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Dimens.minTouchTarget),
        ) { Text(confirmLabel) }
    }
}

@Composable
private fun SheetTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        modifier = modifier.semantics { heading() },
    )
}
