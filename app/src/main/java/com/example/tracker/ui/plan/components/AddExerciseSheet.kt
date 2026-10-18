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
import androidx.compose.material.icons.outlined.TravelExplore
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
import com.example.tracker.ui.common.ExerciseTypeSelector
import com.example.tracker.ui.common.IconAvatar
import com.example.tracker.ui.common.icon
import com.example.tracker.ui.common.label
import com.example.tracker.ui.plan.ExerciseOptionUi
import com.example.tracker.ui.theme.Dimens
import com.example.tracker.ui.theme.spacing

/**
 * Picks an exercise from the library, creates a new one, or opens the online catalog. Used to
 * add an exercise to the day and, with its own [title] and no catalog, to replace one.
 * Calls [onDismiss] after any of these.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExerciseSheet(
    options: List<ExerciseOptionUi>,
    onAdd: (exerciseId: String) -> Unit,
    onCreate: (name: String, type: ExerciseType) -> Unit,
    onBrowseCatalog: (() -> Unit)?,
    onDismiss: () -> Unit,
    title: String = stringResource(R.string.add_exercise_title),
    emptyText: String = stringResource(R.string.add_exercise_library_empty),
    createLabel: String = stringResource(R.string.add_exercise_confirm),
) {
    var isCreating by rememberSaveable { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        if (isCreating) {
            NewExerciseForm(
                confirmLabel = createLabel,
                onCreate = { name, type ->
                    onCreate(name, type)
                    onDismiss()
                },
            )
        } else {
            ExercisePicker(
                title = title,
                emptyText = emptyText,
                options = options,
                onPick = { id ->
                    onAdd(id)
                    onDismiss()
                },
                onCreateNew = { isCreating = true },
                onBrowseCatalog = onBrowseCatalog?.let { browse ->
                    {
                        onDismiss()
                        browse()
                    }
                },
            )
        }
    }
}

@Composable
private fun ExercisePicker(
    title: String,
    emptyText: String,
    options: List<ExerciseOptionUi>,
    onPick: (String) -> Unit,
    onCreateNew: () -> Unit,
    onBrowseCatalog: (() -> Unit)?,
) {
    LazyColumn(modifier = Modifier.padding(bottom = MaterialTheme.spacing.xl)) {
        item(key = "title") {
            SheetTitle(
                text = title,
                modifier = Modifier.padding(horizontal = MaterialTheme.spacing.lg, vertical = MaterialTheme.spacing.sm),
            )
        }
        if (onBrowseCatalog != null) item(key = "browse") {
            ListItem(
                headlineContent = { Text(stringResource(R.string.add_exercise_browse)) },
                supportingContent = { Text(stringResource(R.string.add_exercise_browse_body)) },
                leadingContent = {
                    IconAvatar(
                        icon = Icons.Outlined.TravelExplore,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable(onClick = onBrowseCatalog),
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
                supportingContent = { Text(option.type.label()) },
                leadingContent = {
                    IconAvatar(
                        icon = option.type.icon,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable { onPick(option.id) },
            )
        }
    }
}

@Composable
private fun NewExerciseForm(confirmLabel: String, onCreate: (String, ExerciseType) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf(ExerciseType.WEIGHT_REPS) }
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
        Button(
            onClick = { onCreate(name, type) },
            enabled = name.isNotBlank(),
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
