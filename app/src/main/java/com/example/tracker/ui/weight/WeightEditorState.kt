package com.example.tracker.ui.weight

import com.example.tracker.domain.model.WeightEntry
import java.time.LocalDate

/** The open "Log weight" dialog. The weight is kept as typed so the user can edit it freely. */
data class WeightEditorState(
    val date: LocalDate,
    val weight: String,
    /** Whether [date] already has an entry: saving replaces it, and it can be deleted. */
    val isExisting: Boolean,
    val weightError: Boolean = false,
    val isSaving: Boolean = false,
)

private const val MAX_WEIGHT_LENGTH = 5

/** Digits with at most one decimal separator and one decimal, e.g. "78.4"; a comma becomes a dot. */
internal fun sanitizeWeight(text: String): String {
    val normalized = text.replace(',', '.').filter { it.isDigit() || it == '.' }
    val dot = normalized.indexOf('.')
    val cleaned = if (dot < 0) {
        normalized
    } else {
        normalized.substring(0, dot + 1) + normalized.substring(dot + 1).replace(".", "").take(1)
    }
    return cleaned.take(MAX_WEIGHT_LENGTH)
}

/** The weight in [text] if it is a number within the allowed range, otherwise null. */
internal fun parseWeight(text: String): Double? =
    text.trim().toDoubleOrNull()?.takeIf { it in WeightEntry.MIN_KG..WeightEntry.MAX_KG }

/** "78.4" for a field: no trailing ".0". */
internal fun Double.toWeightInput(): String = if (this % 1.0 == 0.0) toLong().toString() else toString()
