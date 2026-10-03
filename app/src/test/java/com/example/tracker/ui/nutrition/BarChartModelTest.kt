package com.example.tracker.ui.nutrition

import com.example.tracker.domain.model.DailyNutrition
import com.example.tracker.domain.model.NutritionTargets
import com.example.tracker.ui.nutrition.chart.BarChartModel
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BarChartModelTest {

    private val start = LocalDate.of(2026, 10, 1)
    private val end = LocalDate.of(2026, 10, 10)

    private fun day(offset: Long, calories: Int, protein: Int) = DailyNutrition(start.plusDays(offset), calories, protein)

    @Test
    fun build_nothingLogged_isNull() {
        assertNull(BarChartModel.build(start, end, emptyList(), NutritionMetric.CALORIES, NutritionTargets()))
    }

    @Test
    fun build_placesBarsByDateLeavingGapsForUnloggedDays() {
        val days = listOf(day(9, 2000, 100), day(0, 1000, 150), day(3, 3000, 120))

        val chart = BarChartModel.build(start, end, days, NutritionMetric.CALORIES, NutritionTargets())!!

        assertEquals(10, chart.slotCount)
        assertEquals(listOf(0, 3, 9), chart.bars.map { it.slot })
        assertEquals(listOf(1000, 3000, 2000), chart.bars.map { it.value })
        assertEquals(listOf(0.0, 1000.0, 2000.0, 3000.0), chart.ticks.map { it.value })
        assertEquals(listOf(1f / 3, 1f, 2f / 3), chart.bars.map { it.height })
        assertNull(chart.targetHeight)
    }

    @Test
    fun build_axisReachesTargetAboveAllBars() {
        val days = listOf(day(0, 1800, 100), day(1, 2000, 120))

        val chart = BarChartModel.build(start, end, days, NutritionMetric.PROTEIN, NutritionTargets(2400, 160))!!

        assertEquals(listOf(100, 120), chart.bars.map { it.value })
        assertEquals(160, chart.target)
        assertEquals(listOf(0.0, 100.0, 200.0), chart.ticks.map { it.value })
        assertEquals(0.8f, chart.targetHeight)
    }

    @Test
    fun build_ignoresDaysOutsideTheRange() {
        val days = listOf(day(-1, 5000, 100), day(2, 2000, 100))

        val chart = BarChartModel.build(start, end, days, NutritionMetric.CALORIES, NutritionTargets())!!

        assertEquals(listOf(2), chart.bars.map { it.slot })
    }
}
