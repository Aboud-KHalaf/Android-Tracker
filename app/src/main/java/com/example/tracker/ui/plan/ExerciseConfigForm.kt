package com.example.tracker.ui.plan

import com.example.tracker.domain.model.ExerciseConfig
import com.example.tracker.domain.model.ExerciseType
import com.example.tracker.ui.workout.SetInput
import com.example.tracker.ui.common.toWesternDigits

/**
 * The targets as typed in the "Edit targets" sheet. Blank fields mean "repeat last time";
 * text is kept as typed so half-typed input such as "47." survives until the user is done.
 */
internal data class ExerciseConfigForm(
    val sets: Int,
    val repsText: String,
    val weightText: String,
    val holdText: String,
) {
    val isRepsValid: Boolean get() = repsText.isBlank() || SetInput.parseReps(repsText) != null
    val isWeightValid: Boolean get() = weightText.isBlank() || SetInput.parseWeight(weightText) != null
    val isHoldValid: Boolean get() = holdText.isBlank() || parseHold(holdText) != null

    fun withSets(value: Int) = copy(sets = value.coerceIn(1, ExerciseConfig.MAX_TARGET_SETS))

    /** The configuration to save for an exercise of [type], or null while a field is invalid. */
    fun toConfig(type: ExerciseType): ExerciseConfig? {
        val relevantValid = when (type) {
            ExerciseType.WEIGHT_REPS -> isRepsValid && isWeightValid
            ExerciseType.DURATION -> isHoldValid
        }
        if (!relevantValid) return null
        return ExerciseConfig(
            targetSets = sets,
            targetReps = SetInput.parseReps(repsText),
            targetWeightKg = SetInput.parseWeight(weightText),
            targetDurationSeconds = parseHold(holdText),
        ).forType(type)
    }

    companion object {
        private const val MAX_HOLD_SECONDS = 3_600

        fun from(config: ExerciseConfig) = ExerciseConfigForm(
            sets = config.targetSets,
            repsText = SetInput.formatReps(config.targetReps),
            weightText = SetInput.formatWeight(config.targetWeightKg),
            holdText = config.targetDurationSeconds?.toString().orEmpty(),
        )

        private fun parseHold(text: String): Int? =
            text.toWesternDigits().trim().toIntOrNull()?.takeIf { it in 1..MAX_HOLD_SECONDS }
    }
}
