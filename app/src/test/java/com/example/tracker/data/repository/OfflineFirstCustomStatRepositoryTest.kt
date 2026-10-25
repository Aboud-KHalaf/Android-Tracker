package com.example.tracker.data.repository

import com.example.tracker.data.local.SyncState
import com.example.tracker.data.local.TrackerDatabase
import com.example.tracker.domain.model.CustomStat
import com.example.tracker.domain.model.StatColor
import com.example.tracker.domain.model.StatEntry
import com.example.tracker.domain.model.StatIcon
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
class OfflineFirstCustomStatRepositoryTest {

    private lateinit var database: TrackerDatabase
    private val time = FakeTimeProvider()
    private lateinit var repository: OfflineFirstCustomStatRepository

    private val oct1 = LocalDate.of(2026, 10, 1)
    private val bodyFat = CustomStat("", "Body fat", "%", 1, 15.0, StatIcon.PERCENT, StatColor.ORANGE)

    @Before
    fun setUp() {
        database = inMemoryDatabase()
        repository = OfflineFirstCustomStatRepository(database, time, SequentialIds())
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun createStat_storesItWithANewId() = runTest {
        val id = repository.createStat(bodyFat.copy(name = "  Body fat "))

        assertEquals(bodyFat.copy(id = id), repository.getStat(id))
        val overview = repository.observeOverviews().first().single()
        assertEquals(bodyFat.copy(id = id), overview.stat)
        assertNull(overview.latest)
    }

    @Test(expected = IllegalArgumentException::class)
    fun createStat_blankName_isRejected() = runTest {
        repository.createStat(bodyFat.copy(name = " "))
    }

    @Test
    fun observeOverviews_givesFirstAndLatestEntryEver() = runTest {
        val id = repository.createStat(bodyFat)
        repository.saveEntry(StatEntry(id, oct1.plusDays(4), 19.5))
        repository.saveEntry(StatEntry(id, oct1, 21.0))
        repository.saveEntry(StatEntry(id, oct1.plusDays(9), 18.2))

        val overview = repository.observeOverviews().first().single()

        assertEquals(StatEntry(id, oct1, 21.0), overview.first)
        assertEquals(StatEntry(id, oct1.plusDays(9), 18.2), overview.latest)
    }

    @Test
    fun saveEntry_replacesSameDay_andKeepsStatsApart() = runTest {
        val fat = repository.createStat(bodyFat)
        val waist = repository.createStat(bodyFat.copy(name = "Waist", unit = "cm"))
        repository.saveEntry(StatEntry(fat, oct1, 20.0))
        repository.saveEntry(StatEntry(fat, oct1, 19.8))
        repository.saveEntry(StatEntry(waist, oct1, 84.0))

        assertEquals(listOf(StatEntry(fat, oct1, 19.8)), repository.observeEntries(fat, oct1, oct1).first())
        assertEquals(StatEntry(waist, oct1, 84.0), repository.observeEntry(waist, oct1).first())
        assertTrue(database.customStatDao().pendingEntries().all { it.sync.syncState == SyncState.PENDING })
    }

    @Test
    fun deleteEntry_hidesIt_andSavingAgainRestoresIt() = runTest {
        val id = repository.createStat(bodyFat)
        repository.saveEntry(StatEntry(id, oct1, 20.0))

        repository.deleteEntry(id, oct1)
        assertNull(repository.observeEntry(id, oct1).first())
        assertNull(repository.observeOverviews().first().single().latest)

        repository.saveEntry(StatEntry(id, oct1, 20.0))
        assertEquals(StatEntry(id, oct1, 20.0), repository.observeEntry(id, oct1).first())
    }

    @Test
    fun deleteStat_hidesTheStat() = runTest {
        val id = repository.createStat(bodyFat)
        repository.saveEntry(StatEntry(id, oct1, 20.0))

        repository.deleteStat(id)

        assertTrue(repository.observeOverviews().first().isEmpty())
        assertNull(repository.getStat(id))
    }

    @Test
    fun updateStat_changesItsSettings() = runTest {
        val id = repository.createStat(bodyFat)

        repository.updateStat(bodyFat.copy(id = id, target = null, color = StatColor.TEAL))

        assertEquals(bodyFat.copy(id = id, target = null, color = StatColor.TEAL), repository.getStat(id))
    }

    @Test
    fun reorderStats_keepsTheNewOrder_andNewStatsGoLast() = runTest {
        val fat = repository.createStat(bodyFat)
        val waist = repository.createStat(bodyFat.copy(name = "Waist"))
        val steps = repository.createStat(bodyFat.copy(name = "Steps"))

        repository.reorderStats(listOf(steps, fat, waist))
        val weight = repository.createStat(bodyFat.copy(name = "Weight"))
        // Editing a stat keeps its place.
        repository.updateStat(bodyFat.copy(id = fat, name = "Fat"))

        assertEquals(listOf(steps, fat, waist, weight), repository.observeOverviews().first().map { it.stat.id })
    }

    @Test(expected = IllegalArgumentException::class)
    fun reorderStats_missingAStat_isRejected() = runTest {
        val fat = repository.createStat(bodyFat)
        repository.createStat(bodyFat.copy(name = "Waist"))

        repository.reorderStats(listOf(fat))
    }
}
