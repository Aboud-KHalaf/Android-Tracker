package com.example.tracker.ui.common

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.tracker.R
import com.example.tracker.domain.model.DatePeriod
import com.example.tracker.ui.theme.spacing
import java.time.LocalDate

/** This month / Last 30 days / a custom range picked from a calendar. */
@Composable
fun PeriodSelector(
    selected: DatePeriod,
    rangeStart: LocalDate,
    rangeEnd: LocalDate,
    today: LocalDate,
    onSelect: (DatePeriod) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showRangePicker by rememberSaveable { mutableStateOf(false) }
    val locale = currentLocale()
    val groupDescription = stringResource(R.string.period_group)

    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = MaterialTheme.spacing.lg)
            .semantics { contentDescription = groupDescription },
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
    ) {
        FilterChip(
            selected = selected == DatePeriod.ThisMonth,
            onClick = { onSelect(DatePeriod.ThisMonth) },
            label = { Text(stringResource(R.string.period_this_month)) },
        )
        FilterChip(
            selected = selected == DatePeriod.Last30Days,
            onClick = { onSelect(DatePeriod.Last30Days) },
            label = { Text(stringResource(R.string.period_last_30_days)) },
        )
        val isCustom = selected is DatePeriod.Custom
        FilterChip(
            selected = isCustom,
            onClick = { showRangePicker = true },
            label = {
                Text(
                    if (isCustom) {
                        stringResource(R.string.period_range, rangeStart.shortDate(locale), rangeEnd.shortDate(locale))
                    } else {
                        stringResource(R.string.period_custom)
                    },
                )
            },
            leadingIcon = { Icon(Icons.Outlined.DateRange, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) },
        )
    }

    if (showRangePicker) {
        RangePickerDialog(
            initialStart = rangeStart,
            initialEnd = rangeEnd,
            latest = today,
            onPick = { from, to ->
                showRangePicker = false
                onSelect(DatePeriod.Custom(from, to))
            },
            onDismiss = { showRangePicker = false },
        )
    }
}

/** Picks a start and end day, up to [latest]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RangePickerDialog(
    initialStart: LocalDate,
    initialEnd: LocalDate,
    latest: LocalDate,
    onPick: (LocalDate, LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val latestMillis = latest.toUtcMillis()
    val selectableDates = remember(latestMillis) {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= latestMillis
            override fun isSelectableYear(year: Int) = year <= latest.year
        }
    }
    val pickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initialStart.toUtcMillis(),
        initialSelectedEndDateMillis = initialEnd.toUtcMillis(),
        selectableDates = selectableDates,
    )
    val start = pickerState.selectedStartDateMillis
    val end = pickerState.selectedEndDateMillis

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                // A single tapped day counts as a one-day range.
                onClick = { start?.let { onPick(it.toLocalDate(), (end ?: it).toLocalDate()) } },
                enabled = start != null,
            ) { Text(stringResource(R.string.action_ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    ) {
        DateRangePicker(state = pickerState, modifier = Modifier.weight(1f))
    }
}
