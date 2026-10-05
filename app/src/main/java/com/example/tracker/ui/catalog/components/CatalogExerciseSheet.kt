package com.example.tracker.ui.catalog.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.example.tracker.R
import com.example.tracker.domain.model.ExerciseType
import com.example.tracker.ui.catalog.CatalogExerciseUi
import com.example.tracker.ui.common.ExerciseTypeSelector
import com.example.tracker.ui.theme.Dimens
import com.example.tracker.ui.theme.spacing

private const val IMAGE_ASPECT_RATIO = 4f / 3f

/**
 * A catalog exercise's picture, muscles, equipment and instructions, with a choice of how
 * its sets are measured and a button that adds it to the plan.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogExerciseSheet(
    exercise: CatalogExerciseUi,
    isAdding: Boolean,
    onAdd: (ExerciseType) -> Unit,
    onDismiss: () -> Unit,
) {
    var type by rememberSaveable(exercise.id) { mutableStateOf(exercise.suggestedType) }
    val spacing = MaterialTheme.spacing

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = spacing.lg, end = spacing.lg, bottom = spacing.xl),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            Text(
                text = exercise.name,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
            if (exercise.imageUrl != null) {
                CatalogExerciseImage(
                    imageUrl = exercise.imageUrl,
                    type = exercise.suggestedType,
                    contentDescription = stringResource(R.string.catalog_image_description, exercise.name),
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(IMAGE_ASPECT_RATIO),
                )
            }
            DetailLine(stringResource(R.string.catalog_muscles), exercise.primaryMuscles)
            DetailLine(stringResource(R.string.catalog_secondary_muscles), exercise.secondaryMuscles)
            DetailLine(stringResource(R.string.catalog_equipment), exercise.equipment)
            if (exercise.description.isNotBlank()) {
                Text(exercise.description, style = MaterialTheme.typography.bodyMedium)
            }
            if (exercise.isInPlan) {
                Text(
                    text = stringResource(R.string.catalog_already_in_plan),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                ExerciseTypeSelector(selected = type, onSelect = { type = it })
                Button(
                    onClick = { onAdd(type) },
                    enabled = !isAdding,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = Dimens.minTouchTarget),
                ) { Text(stringResource(R.string.add_exercise_confirm)) }
            }
        }
    }
}

/** "Equipment" over "Barbell, Bench"; nothing when [values] is empty. */
@Composable
private fun DetailLine(label: String, values: List<String>) {
    if (values.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(values.joinToString(), style = MaterialTheme.typography.bodyLarge)
    }
}
