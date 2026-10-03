package com.example.tracker.domain.model

import java.time.LocalDate

/** Calories and protein eaten on one day. */
data class DailyNutrition(
    val date: LocalDate,
    val calories: Int,
    val proteinGrams: Int,
) {
    companion object {
        /** Upper bounds that catch typos (an extra zero) without limiting anyone real. */
        const val MAX_CALORIES = 20_000
        const val MAX_PROTEIN_GRAMS = 1_000
    }
}

/** Daily goals; null means no target set. */
data class NutritionTargets(
    val calories: Int? = null,
    val proteinGrams: Int? = null,
)
