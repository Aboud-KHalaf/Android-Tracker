package com.example.tracker.domain.catalog

import com.example.tracker.domain.model.CatalogExercise
import com.example.tracker.domain.model.ExerciseType

/** Names that mark an exercise as a timed hold rather than weight × reps. */
private val HoldKeywords = listOf("plank", "hold", "isometric", "wall sit", "dead hang")

private const val CARDIO_CATEGORY = "cardio"

/**
 * How sets of this catalog exercise are most likely measured. The catalog doesn't say, so
 * cardio and static holds are timed and everything else is weight × reps.
 */
fun CatalogExercise.suggestedType(): ExerciseType {
    val lowerName = name.lowercase()
    val isTimed = category.equals(CARDIO_CATEGORY, ignoreCase = true) ||
        HoldKeywords.any { it in lowerName }
    return if (isTimed) ExerciseType.DURATION else ExerciseType.WEIGHT_REPS
}
