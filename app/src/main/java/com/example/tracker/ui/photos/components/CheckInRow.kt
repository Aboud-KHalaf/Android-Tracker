package com.example.tracker.ui.photos.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import com.example.tracker.R
import com.example.tracker.domain.model.PhotoMeasurement
import com.example.tracker.domain.model.ProgressPhoto
import com.example.tracker.ui.common.currentLocale
import com.example.tracker.ui.common.dateWithYear
import com.example.tracker.ui.photos.CheckInUi
import com.example.tracker.ui.stats.formatStatNumber
import com.example.tracker.ui.theme.spacing
import com.example.tracker.ui.theme.tabularNumbers
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.abs

/** One check-in's date, its front, side and back photos, and its measurements, with a menu to edit or delete it. */
@Composable
fun CheckInRow(
    item: CheckInUi,
    onOpenPhoto: (ProgressPhoto) -> Unit,
    onEditMeasurements: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val checkIn = item.checkIn
    val locale = currentLocale()
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val date = checkIn.date.dateWithYear(locale)
            Text(
                text = if (item.sharesDay) {
                    val time = checkIn.takenAt.atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale))
                    stringResource(R.string.photos_check_in_at, date, time)
                } else {
                    date
                },
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            CheckInMenu(hasMeasurements = checkIn.measurements.isNotEmpty(), onEditMeasurements, onDelete)
        }
        PhotoRow(photos = checkIn.photos, onOpenPhoto = onOpenPhoto)
        if (checkIn.measurements.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)) {
                checkIn.measurements.forEach { measurement ->
                    MeasurementLine(measurement, item.changes[measurement])
                }
            }
        }
    }
}

@Composable
private fun CheckInMenu(hasMeasurements: Boolean, onEditMeasurements: () -> Unit, onDelete: () -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Outlined.MoreVert, contentDescription = stringResource(R.string.photos_check_in_options))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(if (hasMeasurements) R.string.measurements_edit else R.string.measurements_add)) },
                leadingIcon = { Icon(Icons.Outlined.Straighten, contentDescription = null) },
                onClick = {
                    expanded = false
                    onEditMeasurements()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.photos_check_in_delete)) },
                leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                onClick = {
                    expanded = false
                    onDelete()
                },
            )
        }
    }
}

/** "Waist ........ 84.5 cm  −1.5", the change since the last check-in that measured it. */
@Composable
private fun MeasurementLine(measurement: PhotoMeasurement, change: Double?) {
    val locale = currentLocale()
    fun amount(value: Double) = formatStatNumber(value, MEASUREMENT_DECIMALS, locale)
    val unit = measurement.unit.takeIf { it.isNotBlank() }?.let { " $it" }.orEmpty()
    Row(modifier = Modifier.semantics(mergeDescendants = true) {}) {
        Text(measurement.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(amount(measurement.value) + unit, style = MaterialTheme.typography.bodyMedium.tabularNumbers())
        if (change != null && amount(abs(change)) != amount(0.0)) {
            Text(
                text = stringResource(if (change > 0) R.string.stat_change_up else R.string.stat_change_down, amount(abs(change))),
                style = MaterialTheme.typography.bodyMedium.tabularNumbers(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = MaterialTheme.spacing.sm),
            )
        }
    }
}

private const val MEASUREMENT_DECIMALS = 1
