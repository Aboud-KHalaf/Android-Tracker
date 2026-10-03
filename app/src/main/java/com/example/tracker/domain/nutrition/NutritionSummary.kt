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
    /** Days within [CALORIE_TOLERANCE] of the calorie target; null without a target. */
    val caloriesOnTargetDays: Int?,
    /** Days at or above the protein target; null without a target. */
    val proteinOnTargetDays: Int?,
) {
    companion object {
        /** A day "hits" the calorie target when within ±10% of it, over or under. */
        const val CALORIE_TOLERANCE = 0.10

        /** Null when there are no logged days. */
        fun of(days: List<DailyNutrition>, targets: NutritionTargets): NutritionSummary? {
            if (days.isEmpty()) return null
            return NutritionSummary(
                loggedDays = days.size,
                averageCalories = days.map { it.calories }.average().roundToInt(),
                averageProteinGrams = days.map { it.proteinGrams }.average().roundToInt(),
                caloriesOnTargetDays = targets.calories?.let { target -> days.count { isCaloriesOnTarget(it.calories, target) } },
                proteinOnTargetDays = targets.proteinGrams?.let { target -> days.count { isProteinOnTarget(it.proteinGrams, target) } },
            )
        }

        fun isCaloriesOnTarget(calories: Int, target: Int): Boolean =
            abs(calories - target) <= target * CALORIE_TOLERANCE

        fun isProteinOnTarget(proteinGrams: Int, target: Int): Boolean = proteinGrams >= target
    }
}
