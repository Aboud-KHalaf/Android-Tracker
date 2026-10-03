package com.example.tracker.data.repository

import com.example.tracker.data.local.TrackerDatabase
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
        val planId = plans.createPlan("Push Day")
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
        val planId = plans.createPlan("Push Day")
        plans.addExercise(planId, "missing")
    }
}
