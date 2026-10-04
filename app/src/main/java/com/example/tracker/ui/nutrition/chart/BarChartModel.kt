package com.example.tracker.ui.nutrition.chart

import com.example.tracker.domain.model.DailyNutrition
import com.example.tracker.domain.model.NutritionTargets
import com.example.tracker.ui.common.chart.AxisTick
import com.example.tracker.ui.common.chart.ChartModel
import com.example.tracker.ui.nutrition.NutritionMetric
import java.time.LocalDate
import kotlin.math.ceil

/** One logged day. [slot] is its position among the range's days; [height] a fraction of the plot. */
data class DayBar(val slot: Int, val date: LocalDate, val value: Int, val height: Float)

/**
 * A bar per logged day across a date range, ready to draw. Days without an entry, or without a
 * value for the metric, leave a gap rather than a zero bar; [bars] is empty when no day has one. The y-axis starts at 0 and reaches past both the tallest bar and the
 * target, in round steps.
 */
data class BarChartModel(
    val start: LocalDate,
    val end: LocalDate,
    /** Calendar days in the range, logged or not. */
    val slotCount: Int,
    val bars: List<DayBar>,
    val ticks: List<AxisTick>,
    val target: Int?,
    /** The target's height as a fraction of the plot; null without a target. */
    val targetHeight: Float?,
) {
    companion object {
        private const val TARGET_INTERVALS = 3

        /** Null when nothing in the range is logged. */
        fun build(
            start: LocalDate,
            end: LocalDate,
            days: List<DailyNutrition>,
            metric: NutritionMetric,
            targets: NutritionTargets,
        ): BarChartModel? {
            val inRange = days.filter { it.date in start..end }
            if (inRange.isEmpty()) return null
            val logged = inRange
                .mapNotNull { day -> metric.valueOf(day)?.let { day.date to it } }
                .sortedBy { (date, _) -> date }
            val target = metric.targetOf(targets)
            val max = maxOf(logged.maxOfOrNull { (_, value) -> value } ?: 0, target ?: 0).toDouble()
            val step = ChartModel.niceStep(max / TARGET_INTERVALS)
            val high = (ceil(max / step) * step).coerceAtLeast(step)

            fun height(value: Double) = (value / high).toFloat()

            return BarChartModel(
                start = start,
                end = end,
                slotCount = (end.toEpochDay() - start.toEpochDay() + 1).toInt(),
                bars = logged.map { (date, value) ->
                    DayBar((date.toEpochDay() - start.toEpochDay()).toInt(), date, value, height(value.toDouble()))
                },
                ticks = (0..Math.round(high / step).toInt()).map { i -> AxisTick(height(i * step), i * step) },
                target = target,
                targetHeight = target?.let { height(it.toDouble()) },
            )
        }
    }
}
