package com.example.tracker.data.repository

import com.example.tracker.data.local.SyncState
import com.example.tracker.data.local.TrackerDatabase
import com.example.tracker.domain.model.WeightEntry
import java.time.LocalDate
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
class OfflineFirstWeightRepositoryTest {

    private lateinit var database: TrackerDatabase
    private val time = FakeTimeProvider()
    private lateinit var repository: OfflineFirstWeightRepository

    private val oct1 = LocalDate.of(2026, 10, 1)

    @Before
    fun setUp() {
        database = inMemoryDatabase()
        repository = OfflineFirstWeightRepository(database, time)
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun saveEntry_storesAndReplacesSameDay() = runTest {
        repository.saveEntry(WeightEntry(oct1, 80.0))
        repository.saveEntry(WeightEntry(oct1, 79.6))

        assertEquals(listOf(WeightEntry(oct1, 79.6)), repository.observeEntries(oct1, oct1).first())
        assertEquals(SyncState.PENDING, database.weightDao().pending().single().sync.syncState)
    }

    @Test
    fun observeLatest_isNewestEntryOutsideAnyRange() = runTest {
        repository.saveEntry(WeightEntry(oct1, 80.0))
        repository.saveEntry(WeightEntry(oct1.plusDays(9), 78.4))
        repository.saveEntry(WeightEntry(oct1.plusDays(3), 79.1))

        assertEquals(WeightEntry(oct1.plusDays(9), 78.4), repository.observeLatest().first())
        assertEquals(listOf(oct1.plusDays(3), oct1), repository.observeEntries(oct1, oct1.plusDays(5)).first().map { it.date })
    }

    @Test
    fun deleteEntry_hidesIt_andSavingAgainRestoresIt() = runTest {
        repository.saveEntry(WeightEntry(oct1, 80.0))

        repository.deleteEntry(oct1)

        assertNull(repository.observeEntry(oct1).first())
        assertNull(repository.observeLatest().first())

        repository.saveEntry(WeightEntry(oct1, 80.2))

        assertEquals(WeightEntry(oct1, 80.2), repository.observeEntry(oct1).first())
    }

    @Test(expected = IllegalArgumentException::class)
    fun saveEntry_belowMinimum_isRejected() = runTest {
        repository.saveEntry(WeightEntry(oct1, 19.9))
    }
}
