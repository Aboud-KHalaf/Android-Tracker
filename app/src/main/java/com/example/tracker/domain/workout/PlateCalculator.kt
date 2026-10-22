package com.example.tracker.domain.workout

import kotlin.math.roundToLong

/** The plates to put on each side of a bar for a weight; [missingKg] is what the plates can't make. */
data class PlateLoad(val barKg: Double, val perSideKg: List<Double>, val missingKg: Double) {
    val loadedKg: Double get() = barKg + perSideKg.sum() * 2
}

/** Works out which plates make a weight on a barbell. */
object PlateCalculator {
    /** A gym's usual plates, heaviest first. */
    val STANDARD_PLATES_KG: List<Double> = listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25)

    /** Common bars: men's Olympic, women's Olympic, and a technique bar. */
    val BARS_KG: List<Double> = listOf(20.0, 15.0, 10.0)

    /**
     * The heaviest plates first, as many as fit, per side; the same on both sides. Null when
     * [totalKg] is less than the bar itself.
     */
    fun load(totalKg: Double, barKg: Double, platesKg: List<Double> = STANDARD_PLATES_KG): PlateLoad? {
        // Grams as whole numbers, so 2.5 and 1.25 add up exactly.
        val total = totalKg.grams()
        val bar = barKg.grams()
        if (total < bar) return null
        var perSideLeft = (total - bar) / 2
        val perSide = mutableListOf<Double>()
        platesKg.sortedDescending().forEach { plate ->
            val grams = plate.grams()
            while (grams in 1..perSideLeft) {
                perSide += plate
                perSideLeft -= grams
            }
        }
        val missing = total - bar - perSide.sumOf { it.grams() } * 2
        return PlateLoad(barKg, perSide, missing / GRAMS_PER_KG)
    }

    private const val GRAMS_PER_KG = 1_000.0

    private fun Double.grams(): Long = (this * GRAMS_PER_KG).roundToLong()
}
