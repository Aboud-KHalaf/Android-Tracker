package com.example.tracker.domain.nutrition

import com.example.tracker.domain.model.DailyNutrition
import com.example.tracker.domain.model.NutritionTargets
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NutritionSummaryTest {

    private val oct1 = LocalDate.of(2026, 10, 1)

    private fun day(offset: Long, calories: Int, protein: Int) = DailyNutrition(oct1.plusDays(offset), calories, protein)

    @Test
    fun of_noDays_isNull() {
        assertNull(NutritionSummary.of(emptyList(), NutritionTargets(2400, 160)))
    }

    @Test
    fun of_averagesOnlyLoggedDays() {
        // Day 1 is missing on purpose: it must not count as a zero.
        val days = listOf(day(0, 2000, 100), day(2, 2500, 151))

        val summary = NutritionSummary.of(days, NutritionTargets())!!

        assertEquals(2, summary.loggedDays)
        assertEquals(2250, summary.averageCalories)
        assertEquals(126, summary.averageProteinGrams) // 125.5 rounds up
        assertNull(summary.caloriesOnTargetDays)
        assertNull(summary.proteinOnTargetDays)
    }

    @Test
    fun of_averagesStepsOverDaysWithSteps_andIsNullWithoutAny() {
        val days = listOf(
            DailyNutrition(oct1, 2000, 100, steps = 8_000),
            DailyNutrition(oct1.plusDays(1), 2000, 100, steps = 11_001),
            DailyNutrition(oct1.plusDays(2), 2000, 100),
        )

        assertEquals(9_501, NutritionSummary.of(days, NutritionTargets())!!.averageSteps) // 9,500.5 rounds up
        assertNull(NutritionSummary.of(listOf(day(0, 2000, 100)), NutritionTargets())!!.averageSteps)
    }

    @Test
    fun of_countsDaysOnTarget() {
        val days = listOf(
            day(0, 2400, 160), // both on target
            day(1, 2160, 159), // calories exactly -10%, protein just under
            day(2, 2700, 200), // calories over +10%
        )

        val summary = NutritionSummary.of(days, NutritionTargets(calories = 2400, proteinGrams = 160))!!

        assertEquals(2, summary.caloriesOnTargetDays)
        assertEquals(2, summary.proteinOnTargetDays)
    }

    @Test
    fun isCaloriesOnTarget_toleratesTenPercentEitherWay() {
        assertTrue(NutritionSummary.isCaloriesOnTarget(2640, 2400))
        assertFalse(NutritionSummary.isCaloriesOnTarget(2641, 2400))
        assertFalse(NutritionSummary.isCaloriesOnTarget(2159, 2400))
    }
}
