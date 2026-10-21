package com.example.tracker.domain.program

import com.example.tracker.data.repository.FakeTimeProvider
import com.example.tracker.domain.model.Program
import com.example.tracker.domain.model.Workout
import com.example.tracker.domain.model.WorkoutPlan
import com.example.tracker.testing.FakeProgramRepository
import com.example.tracker.testing.FakeWorkoutRepository
import java.time.DayOfWeek
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StartNextWorkoutTest {

    // Monday 28 September 2026.
    private val time = FakeTimeProvider(Instant.parse("2026-09-28T16:00:00Z"))
    private val programs = FakeProgramRepository()
    private val workouts = FakeWorkoutRepository()
    private val startNextWorkout = StartNextWorkout(programs, workouts, time)

    private fun day(id: String, weekday: DayOfWeek?, exercises: Int = 3) =
        WorkoutPlan(id, id, exercises, null, programId = "p", dayOfWeek = weekday)

    @Test
    fun startsTheActiveProgramsNextDay() = runTest {
        programs.programs.value = listOf(
            Program("old", "Old", isActive = false, days = listOf(day("old-day", DayOfWeek.MONDAY))),
            Program("p", "PPL", isActive = true, days = listOf(day("push", DayOfWeek.MONDAY), day("pull", DayOfWeek.WEDNESDAY))),
        )

        assertEquals("workout-1", startNextWorkout())
        assertEquals(listOf("push"), workouts.startedPlanIds)
    }

    @Test
    fun workoutInProgress_isResumedInsteadOfStartingAnother() = runTest {
        workouts.activeWorkout.value = Workout("w1", "push", "Push", time.now(), null, emptyList())
        programs.programs.value = listOf(Program("p", "PPL", isActive = true, days = listOf(day("push", DayOfWeek.MONDAY))))

        assertEquals("w1", startNextWorkout())
        assertEquals(emptyList<String>(), workouts.startedPlanIds)
    }

    @Test
    fun noDayWithExercises_startsNothing() = runTest {
        programs.programs.value = listOf(Program("p", "PPL", isActive = true, days = listOf(day("push", DayOfWeek.MONDAY, exercises = 0))))

        assertNull(startNextWorkout())
        assertNull(StartNextWorkout(FakeProgramRepository(), workouts, time)())
    }
}
