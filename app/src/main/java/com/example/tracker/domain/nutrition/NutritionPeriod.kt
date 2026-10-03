package com.example.tracker.domain.nutrition

import java.time.LocalDate

/** The days the nutrition history covers. */
sealed interface NutritionPeriod {
    /** First day of the period. */
    fun start(today: LocalDate): LocalDate

    /** Last day of the period, inclusive. */
    fun end(today: LocalDate): LocalDate

    /** From the 1st of this month to today. */
    data object ThisMonth : NutritionPeriod {
        override fun start(today: LocalDate): LocalDate = today.withDayOfMonth(1)
        override fun end(today: LocalDate): LocalDate = today
    }

    /** The 30 days ending today. */
    data object Last30Days : NutritionPeriod {
        override fun start(today: LocalDate): LocalDate = today.minusDays(29)
        override fun end(today: LocalDate): LocalDate = today
    }

    data class Custom(val from: LocalDate, val to: LocalDate) : NutritionPeriod {
        init {
            require(!to.isBefore(from)) { "Range ends before it starts" }
        }

        override fun start(today: LocalDate): LocalDate = from
        override fun end(today: LocalDate): LocalDate = to
    }
}
