package com.example.tracker.ui.stats.editor.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.example.tracker.R
import com.example.tracker.ui.stats.StatTemplate
import com.example.tracker.ui.stats.imageVector
import com.example.tracker.ui.theme.spacing

/**
 * Ready-made stats to start a new one from; picking one fills in the form, which stays
 * editable. [selected] is the one the form was last filled from. Shows nothing without [suggestions].
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatSuggestions(
    suggestions: List<StatTemplate>,
    selected: StatTemplate?,
    onPick: (StatTemplate) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (suggestions.isEmpty()) return
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
        Text(
            text = stringResource(R.string.stats_suggestions),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.semantics { heading() },
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
            suggestions.forEach { template ->
                FilterChip(
                    selected = template == selected,
                    onClick = { onPick(template) },
                    label = { Text(stringResource(template.nameRes)) },
                    leadingIcon = {
                        Icon(
                            template.icon.imageVector,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize),
                        )
                    },
                )
            }
        }
    }
}
