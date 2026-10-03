package com.example.tracker.domain.model

import java.time.LocalDate

/** A span of calendar days a history screen covers, relative to today. */
sealed interface DatePeriod {
    /** First day of the period. */
    fun start(today: LocalDate): LocalDate

    /** Last day of the period, inclusive. */
    fun end(today: LocalDate): LocalDate

    /** From the 1st of this month to today. */
    data object ThisMonth : DatePeriod {
        override fun start(today: LocalDate): LocalDate = today.withDayOfMonth(1)
        override fun end(today: LocalDate): LocalDate = today
    }

    /** The 30 days ending today. */
    data object Last30Days : DatePeriod {
        override fun start(today: LocalDate): LocalDate = today.minusDays(29)
        override fun end(today: LocalDate): LocalDate = today
    }

    data class Custom(val from: LocalDate, val to: LocalDate) : DatePeriod {
        init {
            require(!to.isBefore(from)) { "Range ends before it starts" }
        }

        override fun start(today: LocalDate): LocalDate = from
        override fun end(today: LocalDate): LocalDate = to
    }
}
