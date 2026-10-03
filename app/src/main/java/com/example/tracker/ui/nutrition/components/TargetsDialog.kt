package com.example.tracker.ui.nutrition.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.example.tracker.R
import com.example.tracker.domain.model.DailyNutrition
import com.example.tracker.domain.model.NutritionTargets
import com.example.tracker.ui.common.currentLocale
import com.example.tracker.ui.nutrition.TargetInput
import com.example.tracker.ui.nutrition.formatAmount
import com.example.tracker.ui.nutrition.parseTarget
import com.example.tracker.ui.nutrition.sanitizeAmount
import com.example.tracker.ui.theme.spacing

/** Edits the daily calorie and protein targets; a blank field removes that target. */
@Composable
fun TargetsDialog(
    targets: NutritionTargets,
    onSave: (NutritionTargets) -> Unit,
    onDismiss: () -> Unit,
) {
    var calories by rememberSaveable { mutableStateOf(targets.calories?.toString().orEmpty()) }
    var protein by rememberSaveable { mutableStateOf(targets.proteinGrams?.toString().orEmpty()) }
    var caloriesError by rememberSaveable { mutableStateOf(false) }
    var proteinError by rememberSaveable { mutableStateOf(false) }
    val locale = currentLocale()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.nutrition_targets_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)) {
                Text(
                    text = stringResource(R.string.nutrition_targets_body),
                    style = MaterialTheme.typography.bodyMedium,
                )
                AmountField(
                    value = calories,
                    onValueChange = {
                        calories = sanitizeAmount(it)
                        caloriesError = false
                    },
                    label = stringResource(R.string.nutrition_calories),
                    suffix = stringResource(R.string.nutrition_unit_kcal),
                    isError = caloriesError,
                    errorText = stringResource(R.string.nutrition_error_target, formatAmount(DailyNutrition.MAX_CALORIES, locale)),
                    imeAction = ImeAction.Next,
                )
                AmountField(
                    value = protein,
                    onValueChange = {
                        protein = sanitizeAmount(it)
                        proteinError = false
                    },
                    label = stringResource(R.string.nutrition_protein),
                    suffix = stringResource(R.string.nutrition_unit_grams),
                    isError = proteinError,
                    errorText = stringResource(R.string.nutrition_error_target, formatAmount(DailyNutrition.MAX_PROTEIN_GRAMS, locale)),
                    imeAction = ImeAction.Done,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val calorieInput = parseTarget(calories, DailyNutrition.MAX_CALORIES)
                    val proteinInput = parseTarget(protein, DailyNutrition.MAX_PROTEIN_GRAMS)
                    caloriesError = calorieInput is TargetInput.Invalid
                    proteinError = proteinInput is TargetInput.Invalid
                    if (calorieInput is TargetInput.Valid && proteinInput is TargetInput.Valid) {
                        onSave(NutritionTargets(calorieInput.value, proteinInput.value))
                    }
                },
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
