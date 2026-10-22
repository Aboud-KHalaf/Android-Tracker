package com.example.tracker.domain.onboarding

import com.example.tracker.domain.model.Exercise
import com.example.tracker.domain.model.ExerciseType
import com.example.tracker.testing.FakeCustomStatRepository
import com.example.tracker.testing.FakeExerciseRepository
import com.example.tracker.testing.FakeProgramRepository
import com.example.tracker.testing.FakeSettingsRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FirstLaunchTest {

    private val settings = FakeSettingsRepository()
    private val exercises = FakeExerciseRepository()
    private val firstLaunch = FirstLaunch(settings, FakeProgramRepository(), exercises, FakeCustomStatRepository())

    @Test
    fun emptyApp_needsOnboarding() = runTest {
        assertTrue(firstLaunch.needsOnboarding())
        assertFalse(settings.onboardingDone.value)
    }

    @Test
    fun alreadyDone_doesNot() = runTest {
        settings.onboardingDone.value = true

        assertFalse(firstLaunch.needsOnboarding())
    }

    @Test
    fun updatingWithData_skipsItForGood() = runTest {
        exercises.exercises.value = listOf(Exercise("bench", "Bench Press", ExerciseType.WEIGHT_REPS))

        assertFalse(firstLaunch.needsOnboarding())
        assertTrue(settings.onboardingDone.value)
    }
}
