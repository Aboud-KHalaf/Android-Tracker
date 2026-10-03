package com.example.tracker.ui.nutrition.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.example.tracker.R
import com.example.tracker.ui.common.currentLocale
import com.example.tracker.ui.common.formatNumber
import com.example.tracker.ui.common.shortDate
import com.example.tracker.ui.nutrition.NutritionMetric
import com.example.tracker.ui.nutrition.chart.BarChartModel
import com.example.tracker.ui.nutrition.formatAmount
import com.example.tracker.ui.theme.spacing
import com.example.tracker.ui.theme.tabularNumbers
import java.util.Locale
import kotlin.math.roundToInt

private val ChartHeight = 160.dp
private val AxisWidth = 44.dp
/** Room for the top and bottom axis labels, which are centred on their gridlines. */
private val PlotTopInset = 8.dp
private val PlotBottomInset = 8.dp
private val PlotHorizontalInset = 8.dp
private val GridWidth = 1.dp
private val TargetWidth = 2.dp
private val TargetDash = 6.dp
private val MaxBarWidth = 16.dp
private val BarCorner = 2.dp
private const val BAR_FILL = 0.6f

/** Where the plot area sits inside the chart box, in px. */
private class PlotArea(val left: Float, val top: Float, val width: Float, val height: Float) {
    val bottom get() = top + height
    fun y(fraction: Float) = top + (1f - fraction) * height
}

/** Calories / Protein toggle over a bar per logged day, with the target as a dashed line. */
@Composable
fun NutritionChartCard(
    chart: BarChartModel,
    metric: NutritionMetric,
    onSelectMetric: (NutritionMetric) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.lg),
        ) {
            MetricSelector(selected = metric, onSelect = onSelectMetric)
            BarChart(chart, metric)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MetricSelector(selected: NutritionMetric, onSelect: (NutritionMetric) -> Unit) {
    val metrics = NutritionMetric.entries
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        metrics.forEachIndexed { index, metric ->
            SegmentedButton(
                selected = metric == selected,
                onClick = { onSelect(metric) },
                shape = SegmentedButtonDefaults.itemShape(index, metrics.size),
            ) { Text(metric.label()) }
        }
    }
}

@Composable
private fun BarChart(chart: BarChartModel, metric: NutritionMetric) {
    val locale = currentLocale()
    val description = chartDescription(chart, metric, locale)
    val colors = MaterialTheme.colorScheme

    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(ChartHeight)
                .clearAndSetSemantics { contentDescription = description },
        ) {
            val density = LocalDensity.current
            val plot = with(density) {
                PlotArea(
                    left = (AxisWidth + PlotHorizontalInset).toPx(),
                    top = PlotTopInset.toPx(),
                    width = (maxWidth - AxisWidth - PlotHorizontalInset * 2).toPx(),
                    height = (ChartHeight - PlotTopInset - PlotBottomInset).toPx(),
                )
            }
            Canvas(Modifier.fillMaxSize()) {
                chart.ticks.forEach { tick ->
                    val y = plot.y(tick.y)
                    drawLine(colors.outlineVariant, Offset(plot.left, y), Offset(plot.left + plot.width, y), GridWidth.toPx())
                }
                val slotWidth = plot.width / chart.slotCount
                val barWidth = (slotWidth * BAR_FILL).coerceAtMost(MaxBarWidth.toPx()).coerceAtLeast(1f)
                val corner = CornerRadius(BarCorner.toPx().coerceAtMost(barWidth / 2))
                chart.bars.forEach { bar ->
                    val top = plot.y(bar.height)
                    drawRoundRect(
                        color = colors.primary,
                        topLeft = Offset(plot.left + (bar.slot + 0.5f) * slotWidth - barWidth / 2, top),
                        size = Size(barWidth, plot.bottom - top),
                        cornerRadius = corner,
                    )
                }
                chart.targetHeight?.let { height ->
                    val y = plot.y(height)
                    drawLine(
                        color = colors.tertiary,
                        start = Offset(plot.left, y),
                        end = Offset(plot.left + plot.width, y),
                        strokeWidth = TargetWidth.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(TargetDash.toPx(), TargetDash.toPx())),
                    )
                }
            }
            Box(Modifier.width(AxisWidth).fillMaxHeight()) {
                chart.ticks.forEach { tick ->
                    Text(
                        text = formatNumber(tick.value, locale),
                        style = MaterialTheme.typography.labelSmall.tabularNumbers(),
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.placeAt(endX = with(density) { (AxisWidth - 4.dp).toPx() }, centerY = plot.y(tick.y)),
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(start = AxisWidth + PlotHorizontalInset)) {
            val style = MaterialTheme.typography.labelSmall
            Text(chart.start.shortDate(locale), style = style, color = colors.onSurfaceVariant, modifier = Modifier.weight(1f))
            if (chart.end != chart.start) Text(chart.end.shortDate(locale), style = style, color = colors.onSurfaceVariant)
        }
        chart.target?.let { target -> TargetLegend(metric.amountText(target, locale)) }
    }
}

/** A short dashed line and "Target 2,400 kcal". */
@Composable
private fun TargetLegend(amount: String) {
    val color = MaterialTheme.colorScheme.tertiary
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
        modifier = Modifier.padding(start = AxisWidth + PlotHorizontalInset),
    ) {
        Canvas(Modifier.width(TargetDash * 3).height(TargetWidth)) {
            drawLine(
                color = color,
                start = Offset(0f, size.height / 2),
                end = Offset(size.width, size.height / 2),
                strokeWidth = size.height,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(TargetDash.toPx(), TargetDash.toPx())),
            )
        }
        Text(
            text = stringResource(R.string.nutrition_chart_target, amount),
            style = MaterialTheme.typography.labelMedium.tabularNumbers(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun NutritionMetric.label(): String = stringResource(
    when (this) {
        NutritionMetric.CALORIES -> R.string.nutrition_calories
        NutritionMetric.PROTEIN -> R.string.nutrition_protein
    },
)

/** "2,400 kcal" or "160 g". */
@Composable
private fun NutritionMetric.amountText(value: Int, locale: Locale): String = stringResource(
    when (this) {
        NutritionMetric.CALORIES -> R.string.nutrition_kcal
        NutritionMetric.PROTEIN -> R.string.nutrition_grams
    },
    formatAmount(value, locale),
)

/** What a screen reader says for the chart: the metric, the range, the target and every day's value. */
@Composable
private fun chartDescription(chart: BarChartModel, metric: NutritionMetric, locale: Locale): String {
    val values = chart.bars.map { bar ->
        stringResource(R.string.nutrition_chart_point, bar.date.shortDate(locale), metric.amountText(bar.value, locale))
    }.joinToString()
    val target = chart.target?.let { stringResource(R.string.nutrition_chart_target, metric.amountText(it, locale)) }
    return stringResource(
        R.string.nutrition_chart_description,
        metric.label().lowercase(locale),
        chart.start.shortDate(locale),
        chart.end.shortDate(locale),
        listOfNotNull(target, values).joinToString(". "),
    )
}

/** Places a child so its right edge is at [endX] and its middle at [centerY], in px. */
private fun Modifier.placeAt(endX: Float, centerY: Float): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    layout(placeable.width, placeable.height) {
        placeable.place((endX - placeable.width).roundToInt(), (centerY - placeable.height / 2f).roundToInt())
    }
}
