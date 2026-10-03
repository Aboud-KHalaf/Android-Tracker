package com.example.tracker.ui.exercise.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.tracker.R
import com.example.tracker.domain.progress.ProgressMetric
import com.example.tracker.ui.common.currentLocale
import com.example.tracker.ui.common.shortDate
import com.example.tracker.ui.exercise.axisText
import com.example.tracker.ui.exercise.chart.ChartModel
import com.example.tracker.ui.exercise.label
import com.example.tracker.ui.exercise.valueText
import com.example.tracker.ui.theme.spacing
import com.example.tracker.ui.theme.tabularNumbers
import kotlin.math.roundToInt

private val ChartHeight = 160.dp
private val AxisWidth = 40.dp

/** Space above the plot for the last point's label, and below so the lowest dot isn't cut. */
private val PlotTopInset = 32.dp
private val PlotBottomInset = 10.dp
private val PlotHorizontalInset = 8.dp
private val LineWidth = 2.5.dp
private val GridWidth = 1.dp
private val DotRadius = 4.dp
private val LastDotRadius = 6.dp
private val DotStroke = 2.dp
private val LabelGap = 12.dp
private const val AREA_ALPHA = 0.1f

/** Where the plot area sits inside the chart box, in px. */
private class PlotArea(val left: Float, val top: Float, val width: Float, val height: Float) {
    fun x(fraction: Float) = left + fraction * width
    fun y(fraction: Float) = top + (1f - fraction) * height
}

/** Line chart of a metric over time, with a labelled y-axis and first and last dates. */
@Composable
fun ProgressChart(
    chart: ChartModel,
    metric: ProgressMetric,
    modifier: Modifier = Modifier,
) {
    val locale = currentLocale()
    val description = stringResource(
        R.string.chart_description,
        metric.label(),
        chart.first.date.shortDate(locale),
        chart.last.date.shortDate(locale),
        chart.points.map { metric.valueText(it.value) }.joinToString(),
    )
    val tickLabels = chart.ticks.map { metric.axisText(it.value) }
    val lastLabel = stringResource(R.string.chart_point_label, metric.valueText(chart.last.value), chart.last.date.shortDate(locale))

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
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
            ChartCanvas(chart, plot)
            Box(Modifier.width(AxisWidth).fillMaxHeight()) {
                chart.ticks.forEachIndexed { i, tick ->
                    Text(
                        text = tickLabels[i],
                        style = MaterialTheme.typography.labelSmall.tabularNumbers(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.placeAt(endX = with(density) { (AxisWidth - 4.dp).toPx() }, centerY = plot.y(tick.y)),
                    )
                }
            }
            PointLabel(
                text = lastLabel,
                endX = plot.x(chart.last.x),
                bottomY = plot.y(chart.last.y) - with(density) { LabelGap.toPx() },
                maxX = with(density) { maxWidth.toPx() },
            )
        }
        Row(Modifier.fillMaxWidth().padding(start = AxisWidth + PlotHorizontalInset)) {
            val style = MaterialTheme.typography.labelSmall
            val color = MaterialTheme.colorScheme.onSurfaceVariant
            Text(chart.first.date.shortDate(locale), style = style, color = color, modifier = Modifier.weight(1f))
            if (chart.points.size > 1) Text(chart.last.date.shortDate(locale), style = style, color = color)
        }
    }
}

@Composable
private fun ChartCanvas(chart: ChartModel, plot: PlotArea) {
    val colors = MaterialTheme.colorScheme
    Canvas(Modifier.fillMaxSize()) {
        chart.ticks.forEach { tick ->
            val y = plot.y(tick.y)
            drawLine(colors.outlineVariant, Offset(plot.left, y), Offset(plot.left + plot.width, y), GridWidth.toPx())
        }
        val points = chart.points.map { Offset(plot.x(it.x), plot.y(it.y)) }
        if (points.size > 1) {
            val line = Path().apply {
                moveTo(points.first().x, points.first().y)
                points.drop(1).forEach { lineTo(it.x, it.y) }
            }
            val area = Path().apply {
                addPath(line)
                lineTo(points.last().x, plot.top + plot.height)
                lineTo(points.first().x, plot.top + plot.height)
                close()
            }
            drawPath(area, colors.primary.copy(alpha = AREA_ALPHA))
            drawPath(line, colors.primary, style = Stroke(LineWidth.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        points.dropLast(1).forEach { drawDot(it, DotRadius, fill = colors.surface, ring = colors.primary) }
        drawDot(points.last(), LastDotRadius, fill = colors.primary, ring = colors.surface)
    }
}

private fun DrawScope.drawDot(center: Offset, radius: Dp, fill: Color, ring: Color) {
    drawCircle(fill, radius.toPx(), center)
    drawCircle(ring, radius.toPx(), center, style = Stroke(DotStroke.toPx()))
}

/** The last value and date in a small inverse tooltip, ending at [endX] and kept on screen. */
@Composable
private fun PointLabel(text: String, endX: Float, bottomY: Float, maxX: Float) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium.tabularNumbers(),
        color = MaterialTheme.colorScheme.inverseOnSurface,
        modifier = Modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height) {
                    val x = (endX - placeable.width).coerceIn(0f, (maxX - placeable.width).coerceAtLeast(0f))
                    placeable.place(x.roundToInt(), (bottomY - placeable.height).roundToInt().coerceAtLeast(0))
                }
            }
            .background(MaterialTheme.colorScheme.inverseSurface, MaterialTheme.shapes.extraSmall)
            .padding(horizontal = MaterialTheme.spacing.sm, vertical = MaterialTheme.spacing.xs),
    )
}

/** Places a child so its right edge is at [endX] and its middle at [centerY], in px. */
private fun Modifier.placeAt(endX: Float, centerY: Float): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    layout(placeable.width, placeable.height) {
        placeable.place((endX - placeable.width).roundToInt(), (centerY - placeable.height / 2f).roundToInt())
    }
}
