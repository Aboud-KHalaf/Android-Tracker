package com.example.tracker.domain.model

import java.time.LocalDate

/** Body weight on one day, in kilograms. */
data class WeightEntry(
    val date: LocalDate,
    val weightKg: Double,
) {
    companion object {
        /** Bounds that catch typos without limiting anyone real. */
        const val MIN_KG = 20.0
        const val MAX_KG = 400.0
    }
}
