package com.example.tracker.domain.progress

import com.example.tracker.domain.model.CustomStat
import com.example.tracker.domain.model.StatEntry
import com.example.tracker.domain.model.StatIcon
import java.time.LocalDate

/**
 * The stat that tracks bodyweight: one in kilograms with the scale icon, as the Weight
 * suggestion and the old weight log make. The first one, if there are several.
 */
fun List<CustomStat>.bodyweightStat(): CustomStat? =
    firstOrNull { it.icon == StatIcon.SCALE && it.unit.trim().equals(KG, ignoreCase = true) }

private const val KG = "kg"

/** Bodyweight entries over time, to set lifts against. */
class BodyweightLog(entries: List<StatEntry>) {
    private val sorted = entries.filter { it.value > 0.0 }.sortedBy { it.date }

    val isEmpty: Boolean get() = sorted.isEmpty()

    /**
     * Bodyweight on [date]: the latest weigh-in on or before it, or else the first one within
     * [GRACE_DAYS] after it (people often start weighing in a little after they start lifting).
     */
    fun on(date: LocalDate): Double? =
        sorted.lastOrNull { !it.date.isAfter(date) }?.value
            ?: sorted.firstOrNull { !it.date.isAfter(date.plusDays(GRACE_DAYS)) }?.value

    private companion object {
        const val GRACE_DAYS = 7L
    }
}
