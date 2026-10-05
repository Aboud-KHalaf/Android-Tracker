package com.example.tracker.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.tracker.R
import com.example.tracker.domain.model.ExerciseType
import com.example.tracker.ui.theme.spacing

/** "Measured by" label over a segmented choice between the exercise types. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseTypeSelector(
    selected: ExerciseType,
    onSelect: (ExerciseType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
        Text(stringResource(R.string.add_exercise_type_label), style = MaterialTheme.typography.labelLarge)
        val types = ExerciseType.entries
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            types.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    shape = SegmentedButtonDefaults.itemShape(index, types.size),
                ) { Text(option.label()) }
            }
        }
    }
}
