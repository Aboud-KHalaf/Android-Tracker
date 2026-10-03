package com.example.tracker.ui.nutrition

import com.example.tracker.domain.model.DailyNutrition
import java.time.LocalDate

/** The open "Log day" dialog. Amounts are kept as typed so the user can edit them freely. */
data class DayEditorState(
    val date: LocalDate,
    val calories: String,
    val protein: String,
    /** Whether [date] already has an entry: saving replaces it, and it can be deleted. */
    val isExisting: Boolean,
    val caloriesError: Boolean = false,
    val proteinError: Boolean = false,
    val isSaving: Boolean = false,
)

/** Longest amount the fields accept; enough for [DailyNutrition.MAX_CALORIES]. */
internal const val MAX_AMOUNT_LENGTH = 5

/** Keeps only digits, up to [MAX_AMOUNT_LENGTH] of them. */
internal fun sanitizeAmount(text: String): String = text.filter(Char::isDigit).take(MAX_AMOUNT_LENGTH)

/** The whole number in [text] if it is in 0..[max], otherwise null. */
internal fun parseAmount(text: String, max: Int): Int? = text.trim().toIntOrNull()?.takeIf { it in 0..max }
