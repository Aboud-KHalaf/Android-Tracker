package com.example.tracker.ui.exercise.chart

import java.time.LocalDate
import kotlin.math.ceil
import kotlin.math.floor

/** One plotted value. [x] and [y] are fractions of the plot area, y = 0 at the bottom. */
data class ChartPoint(val x: Float, val y: Float, val value: Double, val date: LocalDate)

/** A y-axis gridline and its label value. [y] is a fraction of the plot height from the bottom. */
data class AxisTick(val y: Float, val value: Double)

/** A line chart ready to draw: points left to right, and evenly spaced round-number ticks. */
data class ChartModel(val points: List<ChartPoint>, val ticks: List<AxisTick>) {
    val first: ChartPoint get() = points.first()
    val last: ChartPoint get() = points.last()

    companion object {
        /** Round step sizes for the y-axis, smallest first. */
        private val STEPS = listOf(0.5, 1.0, 2.0, 2.5, 5.0, 10.0, 20.0, 25.0, 50.0, 100.0, 200.0, 250.0, 500.0, 1_000.0, 2_000.0, 2_500.0, 5_000.0, 10_000.0)
        private const val TARGET_INTERVALS = 3

        /**
         * Spaces [values] (oldest first) evenly along x and scales y between round-number
         * bounds, aiming for about three intervals. Null when there is nothing to plot.
         */
        fun build(values: List<Pair<LocalDate, Double>>): ChartModel? {
            if (values.isEmpty()) return null
            val min = values.minOf { it.second }
            val max = values.maxOf { it.second }
            val step = niceStep((max - min) / TARGET_INTERVALS)
            val low = floor(min / step) * step
            var high = ceil(max / step) * step
            if (high == low) high = low + step
            val span = high - low

            val lastIndex = values.lastIndex
            val points = values.mapIndexed { i, (date, value) ->
                val x = if (lastIndex == 0) 0.5f else i.toFloat() / lastIndex
                ChartPoint(x, ((value - low) / span).toFloat(), value, date)
            }
            val tickCount = Math.round(span / step).toInt()
            val ticks = (0..tickCount).map { i ->
                val value = low + i * step
                AxisTick(((value - low) / span).toFloat(), value)
            }
            return ChartModel(points, ticks)
        }

        /** The smallest round step at least [raw]; for a flat line, 1. */
        internal fun niceStep(raw: Double): Double {
            if (raw <= 0.0) return 1.0
            return STEPS.firstOrNull { it >= raw } ?: STEPS.last()
        }
    }
}
