package com.example.tracker.ui.nutrition

import com.example.tracker.domain.model.DailyNutrition
import com.example.tracker.domain.model.NutritionTargets

/** What the nutrition chart plots. */
enum class NutritionMetric {
    CALORIES,
    PROTEIN;

    fun valueOf(day: DailyNutrition): Int = when (this) {
        CALORIES -> day.calories
        PROTEIN -> day.proteinGrams
    }

    fun targetOf(targets: NutritionTargets): Int? = when (this) {
        CALORIES -> targets.calories
        PROTEIN -> targets.proteinGrams
    }
}
