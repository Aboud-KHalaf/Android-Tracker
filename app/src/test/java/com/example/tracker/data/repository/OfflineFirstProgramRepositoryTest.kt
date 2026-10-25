package com.example.tracker.data.repository

import com.example.tracker.data.local.TrackerDatabase
import com.example.tracker.domain.model.ExerciseType
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.WEDNESDAY
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class OfflineFirstProgramRepositoryTest {

    private lateinit var database: TrackerDatabase
    private val time = FakeTimeProvider()
    private val ids = SequentialIds()
    private lateinit var programs: OfflineFirstProgramRepository
    private lateinit var plans: OfflineFirstPlanRepository

    @Before
    fun setUp() {
        database = inMemoryDatabase()
        programs = OfflineFirstProgramRepository(database, time, ids)
        plans = OfflineFirstPlanRepository(database, time, ids)
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun createProgram_addsOneDayPerScheduledWeekdayInOrder() = runTest {
        val id = programs.createProgram("Full Body", mapOf(FRIDAY to "C", MONDAY to "A", WEDNESDAY to "B"))

        val program = programs.observeProgram(id).first()!!

        assertEquals("Full Body", program.name)
        assertEquals(listOf(MONDAY, WEDNESDAY, FRIDAY), program.days.map { it.dayOfWeek })
        assertEquals(listOf("A", "B", "C"), program.days.map { it.name })
        assertEquals(setOf(MONDAY, WEDNESDAY, FRIDAY), program.trainingDays)
    }

    @Test
    fun createProgram_onlyTheFirstBecomesActive() = runTest {
        val first = programs.createProgram("First", emptyMap())
        val second = programs.createProgram("Second", emptyMap())

        val all = programs.observePrograms().first().associateBy { it.id }

        assertTrue(all.getValue(first).isActive)
        assertFalse(all.getValue(second).isActive)
    }

    @Test
    fun setActiveProgram_switchesTheActiveOne() = runTest {
        val first = programs.createProgram("First", emptyMap())
        val second = programs.createProgram("Second", emptyMap())

        programs.setActiveProgram(second)

        val active = programs.observePrograms().first().filter { it.isActive }.map { it.id }
        assertEquals(listOf(second), active)
        assertFalse(programs.observeProgram(first).first()!!.isActive)
    }

    @Test
    fun deleteProgram_removesDaysAndTheirExercises_andHandsOverActive() = runTest {
        val first = programs.createProgram("First", mapOf(MONDAY to "Push"))
        val second = programs.createProgram("Second", emptyMap())
        val dayId = programs.observeProgram(first).first()!!.days.single().id
        val exerciseId = OfflineFirstExerciseRepository(database, time, ids).createExercise("Bench", ExerciseType.WEIGHT_REPS)
        plans.addExercise(dayId, exerciseId)

        programs.deleteProgram(first)

        assertNull(programs.observeProgram(first).first())
        assertNull(plans.observePlan(dayId).first())
        assertTrue(database.planDao().getPlanExercises(dayId).isEmpty())
        assertTrue(programs.observeProgram(second).first()!!.isActive)
    }

    @Test
    fun addDay_onWeekdayOrUnscheduled_sortsScheduledFirst() = runTest {
        val id = programs.createProgram("PPL", mapOf(WEDNESDAY to "Pull"))

        programs.addDay(id, dayOfWeek = null, name = "Extra")
        programs.addDay(id, MONDAY, "Push")

        val days = programs.observeProgram(id).first()!!.days
        assertEquals(listOf("Push", "Pull", "Extra"), days.map { it.name })
        assertEquals(listOf(MONDAY, WEDNESDAY, null), days.map { it.dayOfWeek })
    }

    @Test(expected = IllegalArgumentException::class)
    fun addDay_onTakenWeekday_isRejected() = runTest {
        val id = programs.createProgram("PPL", mapOf(MONDAY to "Push"))
        programs.addDay(id, MONDAY, "Pull")
    }

    @Test
    fun scheduleDay_movesItToAFreeWeekdayOrUnschedulesIt() = runTest {
        val id = programs.createProgram("Split", mapOf(MONDAY to "Upper"))
        val dayId = programs.observeProgram(id).first()!!.days.single().id

        programs.scheduleDay(dayId, FRIDAY)
        assertEquals(FRIDAY, plans.observePlans().first().single().dayOfWeek)

        programs.scheduleDay(dayId, null)
        assertNull(plans.observePlans().first().single().dayOfWeek)
    }

    @Test(expected = IllegalArgumentException::class)
    fun scheduleDay_ontoTakenWeekday_isRejected() = runTest {
        val id = programs.createProgram("Split", mapOf(MONDAY to "Upper", FRIDAY to "Lower"))
        val upper = programs.observeProgram(id).first()!!.days.first().id
        programs.scheduleDay(upper, FRIDAY)
    }

    @Test
    fun deletingADay_freesItsWeekday() = runTest {
        val id = programs.createProgram("Split", mapOf(MONDAY to "Upper"))
        plans.deletePlan(programs.observeProgram(id).first()!!.days.single().id)

        programs.addDay(id, MONDAY, "Lower")

        assertEquals(listOf("Lower"), programs.observeProgram(id).first()!!.days.map { it.name })
    }

    @Test
    fun reorderDays_isKeptWhenTheProgramIsReadAgain() = runTest {
        val id = programs.createProgram("PPL", mapOf(MONDAY to "Push", WEDNESDAY to "Pull", FRIDAY to "Legs"))
        val days = programs.observeProgram(id).first()!!.days.map { it.id }

        programs.reorderDays(id, listOf(days[2], days[0], days[1]))

        val reopened = OfflineFirstProgramRepository(database, time, ids).observeProgram(id).first()!!
        assertEquals(listOf("Legs", "Push", "Pull"), reopened.days.map { it.name })
    }

    @Test(expected = IllegalArgumentException::class)
    fun reorderDays_missingADay_isRejected() = runTest {
        val id = programs.createProgram("PPL", mapOf(MONDAY to "Push", WEDNESDAY to "Pull"))
        val days = programs.observeProgram(id).first()!!.days.map { it.id }

        programs.reorderDays(id, listOf(days[0]))
    }
}
