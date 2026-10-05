package com.example.tracker.domain.catalog

import com.example.tracker.domain.model.Exercise
import com.example.tracker.domain.model.ExerciseType
import com.example.tracker.testing.FakeExerciseRepository
import com.example.tracker.testing.FakePlanRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AddCatalogExerciseToPlanTest {

    private val exercises = FakeExerciseRepository()
    private val plans = FakePlanRepository()
    private val addToPlan = AddCatalogExerciseToPlan(exercises, plans)

    @Test
    fun newName_createsLibraryExerciseAndAddsIt() = runTest {
        val id = addToPlan("push", " Bench Press ", ExerciseType.WEIGHT_REPS)

        assertEquals(listOf("Bench Press" to ExerciseType.WEIGHT_REPS), exercises.createdExercises)
        assertEquals(listOf("add push $id"), plans.writes)
    }

    @Test
    fun existingName_reusesLibraryExerciseIgnoringCase() = runTest {
        exercises.exercises.value = listOf(Exercise("plank", "Plank", ExerciseType.DURATION))

        val id = addToPlan("push", "plank", ExerciseType.WEIGHT_REPS)

        assertEquals("plank", id)
        assertTrue(exercises.createdExercises.isEmpty())
        assertEquals(listOf("add push plank"), plans.writes)
    }

    @Test(expected = IllegalArgumentException::class)
    fun blankName_isRejected() = runTest {
        addToPlan("push", " ", ExerciseType.WEIGHT_REPS)
    }
}
