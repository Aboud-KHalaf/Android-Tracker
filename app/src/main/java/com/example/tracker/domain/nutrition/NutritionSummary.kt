package com.example.tracker.domain.nutrition

import com.example.tracker.domain.model.DailyNutrition
import com.example.tracker.domain.model.NutritionTargets
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Averages and target hits over a set of logged days. Days without an entry aren't
 * counted: not logging a day means "unknown", not "ate nothing".
 */
data class NutritionSummary(
    val loggedDays: Int,
    val averageCalories: Int,
    val averageProteinGrams: Int,
    /** Averaged over the days whose steps were logged; null when none were. */
    val averageSteps: Int?,
    /** Days whose steps were logged; the other days don't count toward the steps target. */
    val stepsLoggedDays: Int,
    /** Days within [CALORIE_TOLERANCE] of the calorie target; null without a target. */
    val caloriesOnTargetDays: Int?,
    /** Days at or above the protein target; null without a target. */
    val proteinOnTargetDays: Int?,
    /** Days with steps at or above the steps target; null without a target or logged steps. */
    val stepsOnTargetDays: Int?,
) {
    companion object {
        /** A day "hits" the calorie target when within ±10% of it, over or under. */
        const val CALORIE_TOLERANCE = 0.10

        /** Null when there are no logged days. */
        fun of(days: List<DailyNutrition>, targets: NutritionTargets): NutritionSummary? {
            if (days.isEmpty()) return null
            val steps = days.mapNotNull { it.steps }
            return NutritionSummary(
                loggedDays = days.size,
                averageCalories = days.map { it.calories }.average().roundToInt(),
                averageProteinGrams = days.map { it.proteinGrams }.average().roundToInt(),
                averageSteps = steps.takeIf { it.isNotEmpty() }?.average()?.roundToInt(),
                stepsLoggedDays = steps.size,
                caloriesOnTargetDays = targets.calories?.let { target -> days.count { isCaloriesOnTarget(it.calories, target) } },
                proteinOnTargetDays = targets.proteinGrams?.let { target -> days.count { isProteinOnTarget(it.proteinGrams, target) } },
                stepsOnTargetDays = targets.steps
                    ?.takeIf { steps.isNotEmpty() }
                    ?.let { target -> steps.count { isStepsOnTarget(it, target) } },
            )
        }

        fun isCaloriesOnTarget(calories: Int, target: Int): Boolean =
            abs(calories - target) <= target * CALORIE_TOLERANCE

        fun isProteinOnTarget(proteinGrams: Int, target: Int): Boolean = proteinGrams >= target

        fun isStepsOnTarget(steps: Int, target: Int): Boolean = steps >= target
    }
}
