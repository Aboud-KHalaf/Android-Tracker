package com.example.tracker.ui.exercise.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.Accessibility
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.tracker.R
import com.example.tracker.domain.model.MuscleGroup
import com.example.tracker.ui.common.label

/** The exercise's muscle group; tapping it offers the others, or none. */
@Composable
fun MuscleGroupChip(selected: MuscleGroup?, onSelect: (MuscleGroup?) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val description = stringResource(R.string.exercise_muscle_group, selected.label())
    Box(modifier) {
        AssistChip(
            onClick = { expanded = true },
            label = { Text(selected.label()) },
            leadingIcon = { Icon(Icons.Outlined.Accessibility, contentDescription = null) },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
            modifier = Modifier.semantics { contentDescription = description },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            (MuscleGroup.entries + null).forEach { group ->
                DropdownMenuItem(
                    text = { Text(group.label()) },
                    leadingIcon = if (group == selected) {
                        { Icon(Icons.Outlined.Check, contentDescription = null) }
                    } else {
                        null
                    },
                    onClick = {
                        expanded = false
                        onSelect(group)
                    },
                )
            }
        }
    }
}
