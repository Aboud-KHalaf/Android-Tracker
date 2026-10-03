package com.example.tracker.ui.common

import com.example.tracker.ui.common.chart.ChartModel
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChartModelTest {

    private fun day(n: Int) = LocalDate.of(2026, 8, 10).plusDays(n * 7L)

    @Test
    fun build_roundsAxisToNiceSteps() {
        // Bench Press top sets from the design: 40 → 50 kg.
        val values = listOf(40.0, 40.0, 42.5, 42.5, 45.0, 45.0, 45.0, 50.0).mapIndexed { i, v -> day(i) to v }

        val chart = ChartModel.build(values)!!

        assertEquals(listOf(40.0, 45.0, 50.0), chart.ticks.map { it.value })
        assertEquals(listOf(0f, 0.5f, 1f), chart.ticks.map { it.y })
        assertEquals(0f, chart.first.x)
        assertEquals(0f, chart.first.y)
        assertEquals(1f, chart.last.x)
        assertEquals(1f, chart.last.y)
        assertEquals(0.25f, chart.points[2].y)
    }

    @Test
    fun build_flatLineGetsOneStepOfHeadroom() {
        val chart = ChartModel.build(listOf(day(0) to 50.0, day(1) to 50.0))!!

        assertEquals(listOf(50.0, 51.0), chart.ticks.map { it.value })
        assertEquals(listOf(0f, 0f), chart.points.map { it.y })
    }

    @Test
    fun build_singlePointIsCentered() {
        val chart = ChartModel.build(listOf(day(0) to 1_040.0))!!

        assertEquals(0.5f, chart.points.single().x)
    }

    @Test
    fun build_largeValuesUseLargerSteps() {
        val chart = ChartModel.build(listOf(day(0) to 900.0, day(1) to 1_200.0))!!

        assertEquals(listOf(900.0, 1_000.0, 1_100.0, 1_200.0), chart.ticks.map { it.value })
    }

    @Test
    fun build_noValues_isNull() {
        assertNull(ChartModel.build(emptyList()))
    }

    @Test
    fun niceStep_picksSmallestRoundStepAtLeastRaw() {
        assertEquals(2.5, ChartModel.niceStep(2.1), 0.0)
        assertEquals(5.0, ChartModel.niceStep(3.3), 0.0)
        assertEquals(1.0, ChartModel.niceStep(0.0), 0.0)
    }
}
