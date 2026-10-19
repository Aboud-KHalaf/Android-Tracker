package com.example.tracker.domain.model

import java.time.LocalDate

/**
 * A number the user tracks once a day, such as body fat or waist size. [target] is optional;
 * whether it means going up or down follows from where the first entry started.
 */
data class CustomStat(
    val id: String,
    val name: String,
    /** Shown after each value, e.g. "%" or "cm"; may be empty. */
    val unit: String,
    /** Decimal places values are entered and shown with, in [DECIMALS_RANGE]. */
    val decimals: Int,
    val target: Double?,
    val icon: StatIcon,
    val color: StatColor,
) {
    companion object {
        const val MAX_NAME_LENGTH = 40
        const val MAX_UNIT_LENGTH = 10
        val DECIMALS_RANGE = 0..2

        /** Bounds that catch typos without limiting any real measurement. */
        const val MIN_VALUE = -1_000_000.0
        const val MAX_VALUE = 1_000_000.0
    }
}

/** A custom stat's value on one day; a day holds at most one per stat. */
data class StatEntry(
    val statId: String,
    val date: LocalDate,
    val value: Double,
)

/** The symbol shown with a custom stat. The UI maps each to an icon. */
enum class StatIcon { CHART, SCALE, RULER, PERCENT, FIRE, WATER, SLEEP, HEART, BOLT, STEPS, FOOD, STAR }

/** The accent a custom stat is drawn in. The theme maps each to light and dark colours. */
enum class StatColor { BLUE, TEAL, GREEN, AMBER, ORANGE, RED, PINK, PURPLE }

/** A stat with its first and latest entries ever, or nulls before the first is logged. */
data class StatOverview(
    val stat: CustomStat,
    val first: StatEntry?,
    val latest: StatEntry?,
)
