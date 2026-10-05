package com.example.tracker.domain.catalog

import com.example.tracker.domain.model.CatalogExercise
import com.example.tracker.domain.model.ExerciseType
import org.junit.Assert.assertEquals
import org.junit.Test

class SuggestedExerciseTypeTest {

    private fun exercise(name: String, category: String? = "Chest") = CatalogExercise(
        id = 1,
        name = name,
        category = category,
        primaryMuscles = emptyList(),
        secondaryMuscles = emptyList(),
        equipment = emptyList(),
        description = "",
        imageUrl = null,
    )

    @Test
    fun liftsAreWeightTimesReps() {
        assertEquals(ExerciseType.WEIGHT_REPS, exercise("Bench Press").suggestedType())
        assertEquals(ExerciseType.WEIGHT_REPS, exercise("Hanging Leg Raise", "Abs").suggestedType())
    }

    @Test
    fun holdsAreTimed() {
        assertEquals(ExerciseType.DURATION, exercise("Side Plank", "Abs").suggestedType())
        assertEquals(ExerciseType.DURATION, exercise("Bench Press Isometric").suggestedType())
        assertEquals(ExerciseType.DURATION, exercise("Wall Sit", "Legs").suggestedType())
    }

    @Test
    fun cardioIsTimed() {
        assertEquals(ExerciseType.DURATION, exercise("Rowing Machine", "Cardio").suggestedType())
    }
}
