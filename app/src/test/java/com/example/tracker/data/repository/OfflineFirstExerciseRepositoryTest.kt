package com.example.tracker.data.repository

import com.example.tracker.data.local.SyncState
import com.example.tracker.data.local.TrackerDatabase
import com.example.tracker.domain.model.ExerciseType
import java.time.Duration
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
class OfflineFirstExerciseRepositoryTest {

    private lateinit var database: TrackerDatabase
    private val time = FakeTimeProvider()
    private lateinit var repository: OfflineFirstExerciseRepository

    @Before
    fun setUp() {
        database = inMemoryDatabase()
        repository = OfflineFirstExerciseRepository(database, time, SequentialIds())
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun create_storesTrimmedNameAndMarksRowPending() = runTest {
        val id = repository.createExercise("  Bench Press ", ExerciseType.WEIGHT_REPS)

        assertEquals("Bench Press", repository.observeExercise(id).first()?.name)
        val pending = database.exerciseDao().pending().single()
        assertEquals(SyncState.PENDING, pending.sync.syncState)
        assertEquals(time.now().toEpochMilli(), pending.sync.createdAt)
    }

    @Test(expected = IllegalArgumentException::class)
    fun create_blankName_isRejected() = runTest {
        repository.createExercise("   ", ExerciseType.WEIGHT_REPS)
    }

    @Test
    fun rename_afterSync_marksRowPendingAgainWithNewTimestamp() = runTest {
        val id = repository.createExercise("Bench", ExerciseType.WEIGHT_REPS)
        database.exerciseDao().markSynced(listOf(id), pushedUpTo = time.now().toEpochMilli())
        assertTrue(database.exerciseDao().pending().isEmpty())

        time.advance(Duration.ofMinutes(5))
        repository.renameExercise(id, "Bench Press")

        val row = database.exerciseDao().pending().single()
        assertEquals(SyncState.PENDING, row.sync.syncState)
        assertEquals(time.now().toEpochMilli(), row.sync.updatedAt)
    }

    @Test
    fun markSynced_keepsRowsChangedAfterThePushPending() = runTest {
        val id = repository.createExercise("Bench", ExerciseType.WEIGHT_REPS)
        val pushedUpTo = time.now().toEpochMilli()
        time.advance(Duration.ofSeconds(1))
        repository.renameExercise(id, "Bench Press")

        database.exerciseDao().markSynced(listOf(id), pushedUpTo)

        assertEquals(listOf(id), database.exerciseDao().pending().map { it.id })
    }

    @Test
    fun delete_hidesExerciseButKeepsPendingTombstone() = runTest {
        val id = repository.createExercise("Plank", ExerciseType.DURATION)

        repository.deleteExercise(id)

        assertTrue(repository.observeExercises().first().isEmpty())
        assertNull(repository.observeExercise(id).first())
        val tombstone = database.exerciseDao().pending().single()
        assertEquals(time.now().toEpochMilli(), tombstone.sync.deletedAt)
    }

    @Test
    fun delete_removesExerciseFromPlans() = runTest {
        val plans = OfflineFirstPlanRepository(database, time, SequentialIds())
        val exerciseId = repository.createExercise("Plank", ExerciseType.DURATION)
        val planId = plans.createPlan("Core")
        plans.addExercise(planId, exerciseId)

        repository.deleteExercise(exerciseId)

        assertTrue(plans.observePlan(planId).first()!!.exercises.isEmpty())
        assertEquals(0, plans.observePlans().first().single().exerciseCount)
    }
}
