package com.example.tracker.ui.program.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.example.tracker.R
import com.example.tracker.ui.program.ProgramSetup
import com.example.tracker.ui.program.ProgramTemplate
import com.example.tracker.ui.theme.Dimens
import com.example.tracker.ui.theme.spacing
import java.time.DayOfWeek

/**
 * "New program": pick a split template, a name and the training days. Choosing a template
 * fills in its name and suggested days, which the user can then change.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateProgramSheet(
    onCreate: (ProgramSetup) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var template by rememberSaveable { mutableStateOf(ProgramTemplate.FULL_BODY) }
    var name by rememberSaveable { mutableStateOf(context.getString(ProgramTemplate.FULL_BODY.nameRes)) }
    var days by rememberSaveable { mutableStateOf(ProgramTemplate.FULL_BODY.suggestedDays.map { it.value }) }
    val spacing = MaterialTheme.spacing

    fun choose(choice: ProgramTemplate) {
        template = choice
        name = if (choice == ProgramTemplate.CUSTOM) "" else context.getString(choice.nameRes)
        days = choice.suggestedDays.map { it.value }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = spacing.lg, end = spacing.lg, bottom = spacing.xl),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            Text(
                text = stringResource(R.string.create_program_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                Text(stringResource(R.string.create_program_split), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    ProgramTemplate.entries.forEach { option ->
                        FilterChip(
                            selected = option == template,
                            onClick = { choose(option) },
                            label = { Text(stringResource(option.nameRes)) },
                        )
                    }
                }
            }
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.program_name_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                Text(stringResource(R.string.create_program_days), style = MaterialTheme.typography.labelLarge)
                WeekdaySelector(
                    selected = days.mapTo(mutableSetOf()) { DayOfWeek.of(it) },
                    onToggle = { day -> days = if (day.value in days) days - day.value else days + day.value },
                )
                Text(
                    text = stringResource(R.string.create_program_days_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(
                onClick = {
                    onCreate(
                        ProgramSetup(
                            name = name,
                            dayLabels = template.dayLabelRes.map(context::getString),
                            days = days.mapTo(mutableSetOf()) { DayOfWeek.of(it) },
                        ),
                    )
                },
                enabled = name.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = Dimens.minTouchTarget),
            ) { Text(stringResource(R.string.create_program_confirm)) }
        }
    }
}
