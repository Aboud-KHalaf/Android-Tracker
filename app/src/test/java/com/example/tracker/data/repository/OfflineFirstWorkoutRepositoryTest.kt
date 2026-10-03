package com.example.tracker.data.repository

import com.example.tracker.data.local.TrackerDatabase
import com.example.tracker.domain.model.ExerciseType
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class OfflineFirstWorkoutRepositoryTest {

    private lateinit var database: TrackerDatabase
    private val time = FakeTimeProvider()
    private val ids = SequentialIds()
    private lateinit var exercises: OfflineFirstExerciseRepository
    private lateinit var plans: OfflineFirstPlanRepository
    private lateinit var workouts: OfflineFirstWorkoutRepository

    private lateinit var planId: String
    private lateinit var benchId: String
    private lateinit var plankId: String

    @Before
    fun setUp() = runTest {
        database = inMemoryDatabase()
        exercises = OfflineFirstExerciseRepository(database, time, ids)
        plans = OfflineFirstPlanRepository(database, time, ids)
        workouts = OfflineFirstWorkoutRepository(database, time, ids)

        planId = plans.createPlan("Push Day")
        benchId = exercises.createExercise("Bench Press", ExerciseType.WEIGHT_REPS)
        plankId = exercises.createExercise("Plank", ExerciseType.DURATION)
        plans.addExercise(planId, benchId, targetSets = 3)
        plans.addExercise(planId, plankId, targetSets = 2)
    }

    @After
    fun tearDown() = database.close()

    /** Starts a workout, logs [benchSets] as completed bench sets, and finishes after 50 min. */
    private suspend fun logWorkout(benchSets: List<Pair<Double, Int>>): String {
        val id = workouts.startWorkout(planId)
        val bench = workouts.observeWorkout(id).first()!!.exercises.first()
        benchSets.forEachIndexed { i, (kg, reps) ->
            val setId = bench.sets.getOrNull(i)?.id ?: workouts.addSet(bench.id)
            workouts.updateSet(setId, kg, reps, null)
            workouts.completeSet(setId)
        }
        time.advance(Duration.ofMinutes(50))
        workouts.finishWorkout(id)
        time.advance(Duration.ofDays(2))
        return id
    }

    @Test
    fun startWorkout_firstTime_createsTargetSetsWithoutValues() = runTest {
        val id = workouts.startWorkout(planId)

        val workout = workouts.observeActiveWorkout().first()!!
        assertEquals(id, workout.id)
        assertEquals("Push Day", workout.name)
        assertEquals(listOf("Bench Press", "Plank"), workout.exercises.map { it.exercise.name })
        assertEquals(listOf(3, 2), workout.exercises.map { it.sets.size })
        assertTrue(workout.exercises.flatMap { it.sets }.all { it.weightKg == null && !it.isCompleted })
    }

    @Test
    fun startWorkout_prefillsSetsFromLastTime() = runTest {
        logWorkout(listOf(45.0 to 10, 50.0 to 8))

        val id = workouts.startWorkout(planId)

        val bench = workouts.observeWorkout(id).first()!!.exercises.first()
        // Third set had no counterpart last time, so it copies the last one.
        assertEquals(listOf(45.0, 50.0, 50.0), bench.sets.map { it.weightKg })
        assertEquals(listOf(10, 8, 8), bench.sets.map { it.reps })
        assertEquals(listOf(45.0 to 10, 50.0 to 8), workouts.lastTimeSets(benchId).map { it.weightKg to it.reps })
    }

    @Test(expected = IllegalStateException::class)
    fun startWorkout_whileOneIsInProgress_isRejected() = runTest {
        workouts.startWorkout(planId)
        workouts.startWorkout(planId)
    }

    @Test
    fun finishWorkout_movesItToHistoryWithPersonalBestCount() = runTest {
        logWorkout(listOf(40.0 to 10))
        val second = logWorkout(listOf(42.5 to 8))

        assertNull(workouts.observeActiveWorkout().first())
        val history = workouts.observeHistory().first()
        assertEquals(listOf(second, history.last().id), history.map { it.id })
        assertEquals(listOf(1, 0), history.map { it.personalBestCount })
        assertEquals(Duration.ofMinutes(50), history.first().duration)
        assertEquals(2, history.first().exerciseCount)
    }

    @Test
    fun uncompletedSets_doNotCountTowardsProgress() = runTest {
        val id = workouts.startWorkout(planId)
        val bench = workouts.observeWorkout(id).first()!!.exercises.first()
        workouts.updateSet(bench.sets[0].id, 100.0, 5, null) // never completed
        workouts.finishWorkout(id)

        assertTrue(workouts.observeExerciseSessions(benchId).first().isEmpty())
        assertTrue(workouts.lastTimeSets(benchId).isEmpty())
    }

    @Test
    fun reopenAndDeleteSet_updateTheWorkout() = runTest {
        val id = workouts.startWorkout(planId)
        val set = workouts.observeWorkout(id).first()!!.exercises.first().sets.first()

        workouts.completeSet(set.id)
        workouts.reopenSet(set.id)
        workouts.deleteSet(set.id)

        val bench = workouts.observeWorkout(id).first()!!.exercises.first()
        assertEquals(2, bench.sets.size)
        assertTrue(bench.sets.none { it.id == set.id })
    }

    @Test
    fun addSet_copiesPreviousWeightAndReps() = runTest {
        val id = workouts.startWorkout(planId)
        val bench = workouts.observeWorkout(id).first()!!.exercises.first()
        workouts.updateSet(bench.sets.last().id, 50.0, 8, null)

        val newSetId = workouts.addSet(bench.id)

        val newSet = workouts.observeWorkout(id).first()!!.exercises.first().sets.last()
        assertEquals(newSetId, newSet.id)
        assertEquals(3, newSet.position)
        assertEquals(50.0 to 8, newSet.weightKg to newSet.reps)
    }

    @Test(expected = IllegalArgumentException::class)
    fun updateSet_negativeWeight_isRejected() = runTest {
        val id = workouts.startWorkout(planId)
        val set = workouts.observeWorkout(id).first()!!.exercises.first().sets.first()
        workouts.updateSet(set.id, -1.0, 5, null)
    }

    @Test
    fun discardWorkout_hidesItAndTombstonesEverything() = runTest {
        val id = workouts.startWorkout(planId)

        workouts.discardWorkout(id)

        assertNull(workouts.observeActiveWorkout().first())
        assertNull(workouts.observeWorkout(id).first())
        val dao = database.workoutDao()
        assertTrue(dao.pendingWorkouts().all { it.sync.deletedAt != null })
        assertTrue(dao.pendingWorkoutExercises().all { it.sync.deletedAt != null })
        assertEquals(5, dao.pendingSets().count { it.sync.deletedAt != null })
    }

    @Test
    fun weekSummary_andPlanLastDone_reflectFinishedWorkouts() = runTest {
        // FakeTimeProvider starts on Monday 2026-09-28 16:00 UTC; logWorkout adds 2 days each time.
        logWorkout(listOf(40.0 to 10))
        logWorkout(listOf(40.0 to 10))

        val summary = workouts.observeWeekSummary(LocalDate.of(2026, 9, 28)).first()

        assertEquals(2, summary.workoutCount)
        assertEquals(Duration.ofMinutes(100), summary.totalDuration)
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), summary.trainedDays)
        assertEquals(
            workouts.observeHistory().first().first().finishedAt,
            plans.observePlans().first().single().lastDoneAt,
        )
    }

    @Test
    fun personalBests_spanAllHistory() = runTest {
        logWorkout(listOf(40.0 to 10))
        val pbWorkout = logWorkout(listOf(45.0 to 6))
        logWorkout(listOf(45.0 to 8))

        val bests = workouts.observePersonalBests().first()

        assertEquals(listOf(pbWorkout), bests.map { it.workoutId })
        assertEquals(3, workouts.observeExerciseSessions(benchId).first().size)
    }
}
