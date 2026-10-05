package com.example.tracker.ui.nutrition

import com.example.tracker.domain.model.DailyNutrition
import com.example.tracker.domain.model.NutritionTargets

/** What the nutrition chart plots. */
enum class NutritionMetric {
    CALORIES,
    PROTEIN,
    STEPS;

    /** Null when [day] has no value for this metric, e.g. its steps weren't logged. */
    fun valueOf(day: DailyNutrition): Int? = when (this) {
        CALORIES -> day.calories
        PROTEIN -> day.proteinGrams
        STEPS -> day.steps
    }

    /** Steps have no target. */
    fun targetOf(targets: NutritionTargets): Int? = when (this) {
        CALORIES -> targets.calories
        PROTEIN -> targets.proteinGrams
        STEPS -> null
    }
}
