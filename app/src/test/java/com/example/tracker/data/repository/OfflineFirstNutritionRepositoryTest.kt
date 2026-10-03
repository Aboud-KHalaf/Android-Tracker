package com.example.tracker.data.repository

import com.example.tracker.data.local.SyncState
import com.example.tracker.data.local.TrackerDatabase
import com.example.tracker.domain.model.DailyNutrition
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
class OfflineFirstNutritionRepositoryTest {

    private lateinit var database: TrackerDatabase
    private val time = FakeTimeProvider()
    private lateinit var repository: OfflineFirstNutritionRepository

    private val oct1 = LocalDate.of(2026, 10, 1)

    @Before
    fun setUp() {
        database = inMemoryDatabase()
        repository = OfflineFirstNutritionRepository(database, time)
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun saveDay_storesDayAndMarksRowPending() = runTest {
        repository.saveDay(DailyNutrition(oct1, 2300, 150))

        assertEquals(DailyNutrition(oct1, 2300, 150), repository.observeDay(oct1).first())
        val pending = database.nutritionDao().pending().single()
        assertEquals(SyncState.PENDING, pending.sync.syncState)
        assertEquals(time.now().toEpochMilli(), pending.sync.createdAt)
    }

    @Test
    fun saveDay_sameDate_replacesEntryAndKeepsCreationTime() = runTest {
        repository.saveDay(DailyNutrition(oct1, 2300, 150))
        val created = time.now().toEpochMilli()
        time.advance(Duration.ofHours(1))

        repository.saveDay(DailyNutrition(oct1, 2500, 170))

        assertEquals(listOf(DailyNutrition(oct1, 2500, 170)), repository.observeDays(oct1, oct1).first())
        val row = database.nutritionDao().pending().single()
        assertEquals(created, row.sync.createdAt)
        assertEquals(time.now().toEpochMilli(), row.sync.updatedAt)
    }

    @Test
    fun observeDays_returnsOnlyDaysInRangeNewestFirst() = runTest {
        listOf(0L, 1L, 2L, 5L).forEach { repository.saveDay(DailyNutrition(oct1.plusDays(it), 2000, 100)) }

        val days = repository.observeDays(oct1.plusDays(1), oct1.plusDays(5)).first()

        assertEquals(listOf(oct1.plusDays(5), oct1.plusDays(2), oct1.plusDays(1)), days.map { it.date })
    }

    @Test
    fun deleteDay_hidesDay_andSavingAgainRestoresIt() = runTest {
        repository.saveDay(DailyNutrition(oct1, 2300, 150))

        repository.deleteDay(oct1)

        assertNull(repository.observeDay(oct1).first())
        assertTrue(repository.observeDays(oct1, oct1).first().isEmpty())
        assertEquals(time.now().toEpochMilli(), database.nutritionDao().pending().single().sync.deletedAt)

        repository.saveDay(DailyNutrition(oct1, 1800, 120))

        assertEquals(DailyNutrition(oct1, 1800, 120), repository.observeDay(oct1).first())
        assertNull(database.nutritionDao().pending().single().sync.deletedAt)
    }

    @Test
    fun deleteDay_unknownDate_doesNothing() = runTest {
        repository.deleteDay(oct1)

        assertTrue(database.nutritionDao().pending().isEmpty())
    }

    @Test(expected = IllegalArgumentException::class)
    fun saveDay_negativeCalories_isRejected() = runTest {
        repository.saveDay(DailyNutrition(oct1, -1, 150))
    }

    @Test(expected = IllegalArgumentException::class)
    fun saveDay_proteinAboveMaximum_isRejected() = runTest {
        repository.saveDay(DailyNutrition(oct1, 2000, DailyNutrition.MAX_PROTEIN_GRAMS + 1))
    }
}
