package com.example.tracker.domain.health

import com.example.tracker.data.repository.FakeTimeProvider
import com.example.tracker.domain.model.CustomStat
import com.example.tracker.domain.model.StatColor
import com.example.tracker.domain.model.StatEntry
import com.example.tracker.domain.model.StatIcon
import com.example.tracker.testing.FakeCustomStatRepository
import com.example.tracker.testing.FakeSettingsRepository
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HealthSyncTest {

    private val time = FakeTimeProvider(Instant.parse("2026-10-21T12:00:00Z"))
    private val today = LocalDate.of(2026, 10, 21)
    private val stats = FakeCustomStatRepository()
    private val settings = FakeSettingsRepository()
    private val source = FakeHealthSource()

    private fun stat(name: String, unit: String, decimals: Int, icon: StatIcon) =
        CustomStat("", name, unit, decimals, null, icon, StatColor.BLUE)

    private val sync = HealthSync(source, stats, settings, time) { metric ->
        when (metric) {
            HealthMetric.STEPS -> stat("Steps", "steps", 0, StatIcon.STEPS)
            HealthMetric.WEIGHT -> stat("Weight", "kg", 1, StatIcon.SCALE)
            HealthMetric.SLEEP -> stat("Sleep", "h", 1, StatIcon.SLEEP)
        }
    }

    private class FakeHealthSource : HealthDataSource {
        var availability = HealthAvailability.AVAILABLE
        var granted = HealthMetric.entries.toSet()
        val data = mutableMapOf<HealthMetric, List<DailyValue>>()
        val reads = mutableListOf<Pair<LocalDate, LocalDate>>()

        override fun availability() = availability
        override suspend fun grantedMetrics() = granted
        override suspend fun readDaily(metric: HealthMetric, from: LocalDate, to: LocalDate): List<DailyValue> {
            reads += from to to
            return data[metric].orEmpty()
        }
    }

    private fun entriesOf(name: String) =
        stats.stats.value.first { it.name == name }.id.let { id -> stats.entries.value.values.filter { it.statId == id } }

    @Test
    fun sync_makesMissingStatsAndFillsTheLast30Days() = runTest {
        source.data[HealthMetric.STEPS] = listOf(DailyValue(today, 8_432.0))
        source.data[HealthMetric.SLEEP] = listOf(DailyValue(today, 7.4166))

        assertEquals(2, sync.sync())

        assertEquals(listOf(StatEntry(entriesOf("Steps").single().statId, today, 8_432.0)), entriesOf("Steps"))
        assertEquals(7.4, entriesOf("Sleep").single().value, 0.0)
        // No weight data, so no Weight stat is made.
        assertEquals(listOf("Steps", "Sleep"), stats.stats.value.map { it.name })
        assertEquals(today.minusDays(29) to today, source.reads.first())
        assertEquals(time.now(), settings.lastHealthSync.value)
    }

    @Test
    fun sync_usesTheExistingBodyweightStat_andKeepsDaysWithoutHealthData() = runTest {
        val weightId = stats.createStat(stat("My weight", " KG", 1, StatIcon.SCALE))
        stats.saveEntry(StatEntry(weightId, today.minusDays(3), 80.0))
        stats.saveEntry(StatEntry(weightId, today, 79.0))
        source.data[HealthMetric.WEIGHT] = listOf(DailyValue(today, 78.64))

        sync.sync()

        assertEquals(listOf("My weight"), stats.stats.value.map { it.name })
        assertEquals(
            mapOf(today.minusDays(3) to 80.0, today to 78.6),
            entriesOf("My weight").associate { it.date to it.value },
        )
    }

    @Test
    fun sync_readsOnlyAllowedMetrics() = runTest {
        source.granted = setOf(HealthMetric.STEPS)
        source.data[HealthMetric.STEPS] = listOf(DailyValue(today, 100.0))
        source.data[HealthMetric.SLEEP] = listOf(DailyValue(today, 8.0))

        sync.sync()

        assertEquals(listOf("Steps"), stats.stats.value.map { it.name })
    }

    @Test
    fun sync_withoutHealthConnect_doesNothing() = runTest {
        source.availability = HealthAvailability.NEEDS_INSTALL

        assertEquals(0, sync.sync())
        assertNull(settings.lastHealthSync.value)
    }

    @Test
    fun syncIfDue_onlyWhenOnAndNotJustDone() = runTest {
        source.data[HealthMetric.STEPS] = listOf(DailyValue(today, 100.0))

        sync.syncIfDue()
        assertNull(settings.lastHealthSync.value)

        settings.healthSyncEnabled.value = true
        settings.lastHealthSync.value = time.now().minus(Duration.ofMinutes(5))
        sync.syncIfDue()
        assertEquals(0, source.reads.size)

        settings.lastHealthSync.value = time.now().minus(Duration.ofMinutes(20))
        sync.syncIfDue()
        assertEquals(time.now(), settings.lastHealthSync.value)
    }
}
