package com.example.tracker.ui.program.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.tracker.ui.common.currentLocale
import com.example.tracker.ui.common.fullName
import com.example.tracker.ui.common.shortName
import com.example.tracker.ui.theme.spacing
import java.time.DayOfWeek

/** One chip per weekday, Monday first, wrapping onto a second line on narrow screens. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WeekdaySelector(
    selected: Set<DayOfWeek>,
    onToggle: (DayOfWeek) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val locale = currentLocale()
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
    ) {
        DayOfWeek.entries.forEach { day ->
            val isSelected = day in selected
            val fullName = day.fullName(locale)
            FilterChip(
                selected = isSelected,
                onClick = { onToggle(day) },
                enabled = enabled,
                label = { Text(day.shortName(locale)) },
                leadingIcon = if (isSelected) {
                    { Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
                } else {
                    null
                },
                // Read the full name, e.g. "Monday", rather than "Mon".
                modifier = Modifier.semantics { contentDescription = fullName },
            )
        }
    }
}
