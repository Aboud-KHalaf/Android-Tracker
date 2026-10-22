package com.example.tracker.data.repository

import com.example.tracker.data.local.TrackerDatabase
import com.example.tracker.domain.model.ExerciseConfig
import com.example.tracker.domain.model.ExerciseType
import java.time.Duration
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class OfflineFirstPlanRepositoryTest {

    private lateinit var database: TrackerDatabase
    private val time = FakeTimeProvider()
    private val ids = SequentialIds()
    private lateinit var exercises: OfflineFirstExerciseRepository
    private lateinit var plans: OfflineFirstPlanRepository

    @Before
    fun setUp() {
        database = inMemoryDatabase()
        exercises = OfflineFirstExerciseRepository(database, time, ids)
        plans = OfflineFirstPlanRepository(database, time, ids)
    }

    @After
    fun tearDown() = database.close()

    private suspend fun pushDayWith(vararg names: String): String {
        val planId = OfflineFirstProgramRepository(database, time, ids).newDay("Push Day")
        names.forEach { plans.addExercise(planId, exercises.createExercise(it, ExerciseType.WEIGHT_REPS)) }
        return planId
    }

    @Test
    fun addExercise_appendsInOrderAndCountsInSummary() = runTest {
        val planId = pushDayWith("Bench Press", "Overhead Press")

        val plan = plans.observePlan(planId).first()!!

        assertEquals(listOf("Bench Press", "Overhead Press"), plan.exercises.map { it.exercise.name })
        assertEquals(listOf(0, 1), plan.exercises.map { it.position })
        assertEquals(2, plans.observePlans().first().single().exerciseCount)
    }

    @Test
    fun moveExercise_reordersAndTouchesOnlyMovedRows() = runTest {
        val planId = pushDayWith("A", "B", "C")
        val before = database.planDao().getPlanExercises(planId)
        time.advance(Duration.ofMinutes(1))

        plans.moveExercise(planId, fromIndex = 2, toIndex = 0)

        val after = plans.observePlan(planId).first()!!.exercises
        assertEquals(listOf("C", "A", "B"), after.map { it.exercise.name })
        val updated = database.planDao().getPlanExercises(planId)
            .filter { it.sync.updatedAt == time.now().toEpochMilli() }
        assertEquals(before.size, updated.size) // all three positions changed
    }

    @Test
    fun moveExercise_adjacentSwap_touchesTwoRows() = runTest {
        val planId = pushDayWith("A", "B", "C")
        time.advance(Duration.ofMinutes(1))

        plans.moveExercise(planId, fromIndex = 0, toIndex = 1)

        val touched = database.planDao().getPlanExercises(planId)
            .count { it.sync.updatedAt == time.now().toEpochMilli() }
        assertEquals(2, touched)
        assertEquals(listOf("B", "A", "C"), plans.observePlan(planId).first()!!.exercises.map { it.exercise.name })
    }

    @Test(expected = IllegalArgumentException::class)
    fun moveExercise_outOfRange_isRejected() = runTest {
        val planId = pushDayWith("A")
        plans.moveExercise(planId, 0, 3)
    }

    @Test
    fun removeExercise_thenRestore_putsItBackInPlace() = runTest {
        val planId = pushDayWith("A", "B", "C")
        val removed = plans.observePlan(planId).first()!!.exercises[1]

        plans.removeExercise(removed.id)
        assertEquals(listOf("A", "C"), plans.observePlan(planId).first()!!.exercises.map { it.exercise.name })

        plans.restoreExercise(removed.id)
        assertEquals(listOf("A", "B", "C"), plans.observePlan(planId).first()!!.exercises.map { it.exercise.name })
    }

    @Test
    fun deletePlan_hidesPlanAndTombstonesItsExercises() = runTest {
        val planId = pushDayWith("A", "B")

        plans.deletePlan(planId)

        assertNull(plans.observePlan(planId).first())
        assertEquals(emptyList<Any>(), plans.observePlans().first())
        val tombstones = database.planDao().pendingPlanExercises()
        assertEquals(2, tombstones.count { it.sync.deletedAt != null })
    }

    @Test(expected = IllegalArgumentException::class)
    fun addExercise_unknownExercise_isRejected() = runTest {
        val planId = OfflineFirstProgramRepository(database, time, ids).newDay("Push Day")
        plans.addExercise(planId, "missing")
    }

    @Test
    fun replaceExercise_keepsPlaceAndConfig_andIsRestoredOnReload() = runTest {
        val planId = pushDayWith("Bench Press", "Overhead Press")
        val bench = plans.observePlan(planId).first()!!.exercises.first()
        plans.updateExerciseConfig(bench.id, ExerciseConfig(targetSets = 5, targetReps = 5, targetWeightKg = 100.0))
        val dumbbell = exercises.createExercise("Dumbbell Press", ExerciseType.WEIGHT_REPS)

        plans.replaceExercise(bench.id, dumbbell)

        // A fresh repository reads only what was persisted.
        val reloaded = OfflineFirstPlanRepository(database, time, ids).observePlan(planId).first()!!.exercises
        assertEquals(listOf("Dumbbell Press", "Overhead Press"), reloaded.map { it.exercise.name })
        assertEquals(bench.id, reloaded.first().id)
        assertEquals(ExerciseConfig(targetSets = 5, targetReps = 5, targetWeightKg = 100.0), reloaded.first().config)
    }

    @Test
    fun replaceExercise_withOtherType_dropsTargetsThatNoLongerApply() = runTest {
        val planId = pushDayWith("Bench Press")
        val bench = plans.observePlan(planId).first()!!.exercises.single()
        plans.updateExerciseConfig(bench.id, ExerciseConfig(targetSets = 4, targetReps = 8, targetWeightKg = 60.0))
        val plank = exercises.createExercise("Plank", ExerciseType.DURATION)

        plans.replaceExercise(bench.id, plank)

        assertEquals(ExerciseConfig(targetSets = 4), plans.observePlan(planId).first()!!.exercises.single().config)
    }

    @Test
    fun updateExerciseConfig_changesOnlyThatExercise() = runTest {
        val planId = pushDayWith("A", "B")
        val (a, b) = plans.observePlan(planId).first()!!.exercises

        plans.updateExerciseConfig(a.id, ExerciseConfig(targetSets = 4, targetReps = 10, targetWeightKg = 42.5))

        val after = plans.observePlan(planId).first()!!.exercises
        assertEquals(ExerciseConfig(targetSets = 4, targetReps = 10, targetWeightKg = 42.5), after[0].config)
        assertEquals(b.config, after[1].config)
    }

    @Test(expected = IllegalArgumentException::class)
    fun updateExerciseConfig_zeroSets_isRejected() = runTest {
        val planId = pushDayWith("A")
        val a = plans.observePlan(planId).first()!!.exercises.single()
        plans.updateExerciseConfig(a.id, ExerciseConfig(targetSets = 0))
    }

    /** Each exercise's superset as a letter in order of appearance, "-" for none: "AA-BB". */
    private suspend fun supersets(planId: String): String {
        val groups = plans.observePlan(planId).first()!!.exercises.map { it.supersetGroup }
        val letters = groups.filterNotNull().distinct().withIndex().associate { (i, group) -> group to 'A' + i }
        return groups.joinToString("") { group -> letters[group]?.toString() ?: "-" }
    }

    private suspend fun exerciseIds(planId: String) = plans.observePlan(planId).first()!!.exercises.map { it.id }

    @Test
    fun supersetWithNext_pairsAndThenGrowsTheSuperset() = runTest {
        val planId = pushDayWith("A", "B", "C", "D")
        val ids = exerciseIds(planId)

        plans.supersetWithNext(ids[0])
        assertEquals("AA--", supersets(planId))

        plans.supersetWithNext(ids[1])
        assertEquals("AAA-", supersets(planId))
    }

    @Test
    fun supersetWithNext_joinsTwoSupersets() = runTest {
        val planId = pushDayWith("A", "B", "C", "D")
        val ids = exerciseIds(planId)
        plans.supersetWithNext(ids[0])
        plans.supersetWithNext(ids[2])
        assertEquals("AABB", supersets(planId))

        plans.supersetWithNext(ids[1])

        assertEquals("AAAA", supersets(planId))
    }

    @Test
    fun supersetWithNext_lastExercise_doesNothing() = runTest {
        val planId = pushDayWith("A", "B")

        plans.supersetWithNext(exerciseIds(planId)[1])

        assertEquals("--", supersets(planId))
    }

    @Test
    fun removeFromSuperset_middleSplitsIt_andALoneExerciseLeavesToo() = runTest {
        val planId = pushDayWith("A", "B", "C", "D")
        val ids = exerciseIds(planId)
        plans.supersetWithNext(ids[0])
        plans.supersetWithNext(ids[1])
        plans.supersetWithNext(ids[2])
        assertEquals("AAAA", supersets(planId))

        plans.removeFromSuperset(ids[1])

        assertEquals("--AA", supersets(planId))
    }

    @Test
    fun startWorkout_copiesSupersets() = runTest {
        val planId = pushDayWith("A", "B", "C")
        plans.supersetWithNext(exerciseIds(planId)[0])
        val workouts = OfflineFirstWorkoutRepository(database, time, ids)

        val workout = workouts.observeWorkout(workouts.startWorkout(planId)).first()!!

        val groups = workout.exercises.sortedBy { it.position }.map { it.supersetGroup }
        assertEquals(groups[0], groups[1])
        assertEquals(true, groups[0] != null)
        assertEquals(null, groups[2])
    }
}
