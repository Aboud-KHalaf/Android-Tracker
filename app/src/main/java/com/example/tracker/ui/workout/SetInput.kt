package com.example.tracker.ui.workout

import kotlin.math.roundToInt

/**
 * Parsing, validation and stepping for the weight and reps a user types into a set.
 * Works on text so half-typed input such as "47." survives until the user finishes typing.
 */
internal object SetInput {
    const val WEIGHT_STEP_KG = 2.5
    const val REPS_STEP = 1
    private const val MAX_WEIGHT_KG = 1_000.0
    private const val MAX_REPS = 999

    /** Kilograms ≥ 0, accepting "," or "." as the decimal separator; null if invalid. */
    fun parseWeight(text: String): Double? =
        text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it in 0.0..MAX_WEIGHT_KG }

    /** Reps ≥ 1 (a set with no reps isn't a set); null if invalid. */
    fun parseReps(text: String): Int? =
        text.trim().toIntOrNull()?.takeIf { it in 1..MAX_REPS }

    /** "50" or "47.5"; empty when there is no value yet. */
    fun formatWeight(kg: Double?): String = when {
        kg == null -> ""
        kg % 1.0 == 0.0 -> kg.toLong().toString()
        else -> ((kg * 100).roundToInt() / 100.0).toString()
    }

    fun formatReps(reps: Int?): String = reps?.toString().orEmpty()

    /** Adds [steps] × 2.5 kg to the typed weight (empty counts as 0), never below 0. */
    fun stepWeight(text: String, steps: Int): String {
        val current = parseWeight(text) ?: 0.0
        return formatWeight((current + steps * WEIGHT_STEP_KG).coerceIn(0.0, MAX_WEIGHT_KG))
    }

    /** Adds [steps] reps to the typed reps (empty counts as 0), never below 1. */
    fun stepReps(text: String, steps: Int): String {
        val current = text.trim().toIntOrNull() ?: 0
        return formatReps((current + steps * REPS_STEP).coerceIn(1, MAX_REPS))
    }
}
