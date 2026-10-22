package com.example.tracker.ui.workout.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import com.example.tracker.R
import com.example.tracker.ui.common.NoteDialog
import com.example.tracker.ui.theme.Dimens
import com.example.tracker.ui.theme.spacing

/** The current exercise's note, or "Add note"; tapping either opens the editor. */
@Composable
fun ExerciseNote(
    exerciseName: String,
    note: String?,
    onSave: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var editing by rememberSaveable { mutableStateOf(false) }
    if (note == null) {
        TextButton(onClick = { editing = true }, modifier = modifier) {
            Icon(Icons.Outlined.EditNote, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
            Text(stringResource(R.string.workout_exercise_note_add))
        }
    } else {
        val editLabel = stringResource(R.string.workout_exercise_note_edit, exerciseName)
        Surface(
            onClick = { editing = true },
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = Dimens.minTouchTarget)
                .semantics { onClick(label = editLabel) { editing = true; true } },
        ) {
            Row(
                modifier = Modifier.padding(MaterialTheme.spacing.md),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(Icons.AutoMirrored.Outlined.Notes, contentDescription = null)
                Text(note, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
    if (editing) {
        NoteDialog(
            title = stringResource(R.string.workout_exercise_note_title, exerciseName),
            placeholder = stringResource(R.string.workout_exercise_note_hint),
            initialNote = note,
            onSave = {
                editing = false
                onSave(it)
            },
            onDismiss = { editing = false },
        )
    }
}
