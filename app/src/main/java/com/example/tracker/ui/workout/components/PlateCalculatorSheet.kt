package com.example.tracker.ui.workout.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.tracker.R
import com.example.tracker.domain.workout.PlateCalculator
import com.example.tracker.domain.workout.PlateLoad
import com.example.tracker.ui.common.currentLocale
import com.example.tracker.ui.common.formatWeight
import com.example.tracker.ui.theme.spacing

private val PlateWidth = 14.dp
private val MinPlateHeight = 28.dp
private val MaxPlateHeight = 96.dp
private val BarHeight = 10.dp
private val BarWidth = 40.dp

/** Shows which plates go on each side of the bar for [weightKg], with a choice of bar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlateCalculatorSheet(weightKg: Double?, onDismiss: () -> Unit) {
    var barKg by rememberSaveable { mutableDoubleStateOf(PlateCalculator.BARS_KG.first()) }
    val locale = currentLocale()
    fun kg(value: Double) = formatWeight(value, locale)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = MaterialTheme.spacing.lg, end = MaterialTheme.spacing.lg, bottom = MaterialTheme.spacing.xl),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.lg),
        ) {
            Text(
                text = weightKg?.let { stringResource(R.string.plates_title, stringResource(R.string.value_kg, kg(it))) }
                    ?: stringResource(R.string.plates_action),
                style = MaterialTheme.typography.titleLarge,
            )
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
                Text(stringResource(R.string.plates_bar), style = MaterialTheme.typography.labelLarge)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    PlateCalculator.BARS_KG.forEachIndexed { index, bar ->
                        SegmentedButton(
                            selected = bar == barKg,
                            onClick = { barKg = bar },
                            shape = SegmentedButtonDefaults.itemShape(index, PlateCalculator.BARS_KG.size),
                        ) { Text(stringResource(R.string.value_kg, kg(bar))) }
                    }
                }
            }
            val load = weightKg?.let { PlateCalculator.load(it, barKg) }
            when {
                weightKg == null -> Text(stringResource(R.string.plates_no_weight))
                load == null -> Text(stringResource(R.string.plates_below_bar, stringResource(R.string.value_kg, kg(barKg))))
                else -> PlateLoadContent(load, ::kg)
            }
        }
    }
}

@Composable
private fun PlateLoadContent(load: PlateLoad, kg: (Double) -> String) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)) {
        BarDrawing(load.perSideKg)
        Text(
            text = if (load.perSideKg.isEmpty()) {
                stringResource(R.string.plates_just_bar)
            } else {
                val separator = stringResource(R.string.plates_separator)
                stringResource(
                    R.string.plates_each_side,
                    stringResource(R.string.value_kg, load.perSideKg.joinToString(separator) { plate -> kg(plate) }),
                )
            },
            style = MaterialTheme.typography.titleMedium,
        )
        if (load.missingKg > 0) {
            Text(
                text = stringResource(R.string.plates_missing, stringResource(R.string.value_kg, kg(load.missingKg))),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

/** One side of the bar: its sleeve with the plates, heaviest nearest the middle. Decorative; the text says it. */
@Composable
private fun BarDrawing(plates: List<Double>) {
    val heaviest = PlateCalculator.STANDARD_PLATES_KG.max()
    Row(
        modifier = Modifier
            .height(MaxPlateHeight)
            .clearAndSetSemantics {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Box(
            Modifier
                .width(BarWidth)
                .height(BarHeight)
                .background(MaterialTheme.colorScheme.outline, RoundedCornerShape(2.dp)),
        )
        plates.forEach { plate ->
            val height: Dp = MinPlateHeight + (MaxPlateHeight - MinPlateHeight) * (plate / heaviest).toFloat()
            Box(
                Modifier
                    .width(PlateWidth)
                    .height(height)
                    .background(plateColor(plate), RoundedCornerShape(3.dp)),
            )
        }
        Box(
            Modifier
                .width(BarWidth)
                .height(BarHeight)
                .background(MaterialTheme.colorScheme.outline, RoundedCornerShape(2.dp)),
        )
    }
}

/** A colour per plate size, from the theme, so it works in light and dark. */
@Composable
private fun plateColor(plate: Double): Color {
    val colors = MaterialTheme.colorScheme
    val palette = listOf(colors.primary, colors.tertiary, colors.secondary, colors.onSurfaceVariant)
    val index = PlateCalculator.STANDARD_PLATES_KG.indexOf(plate).coerceAtLeast(0)
    return palette[index % palette.size]
}
