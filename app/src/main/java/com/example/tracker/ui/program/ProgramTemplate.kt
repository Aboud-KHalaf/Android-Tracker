package com.example.tracker.ui.program

import androidx.annotation.StringRes
import com.example.tracker.R
import java.time.DayOfWeek
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY

/**
 * Starting points for a new program. A template only suggests a name, day names and weekdays;
 * the user can change all of them, so adding a split is adding an entry here.
 */
enum class ProgramTemplate(
    @StringRes val nameRes: Int,
    /** Names given to the chosen days in turn; empty names days after their weekday. */
    val dayLabelRes: List<Int>,
    val suggestedDays: Set<DayOfWeek>,
) {
    FULL_BODY(R.string.template_full_body, listOf(R.string.day_full_body), setOf(MONDAY, WEDNESDAY, FRIDAY)),
    UPPER_LOWER(
        R.string.template_upper_lower,
        listOf(R.string.day_upper, R.string.day_lower),
        setOf(MONDAY, TUESDAY, THURSDAY, FRIDAY),
    ),
    PUSH_PULL_LEGS(
        R.string.template_push_pull_legs,
        listOf(R.string.day_push, R.string.day_pull, R.string.day_legs),
        setOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY),
    ),
    CUSTOM(R.string.template_custom, emptyList(), emptySet()),
}

/** What the user chose in the "New program" sheet, with the template's texts resolved. */
data class ProgramSetup(
    val name: String,
    val dayLabels: List<String>,
    val days: Set<DayOfWeek>,
)
