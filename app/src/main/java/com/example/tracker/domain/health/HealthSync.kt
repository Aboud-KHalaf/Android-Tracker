package com.example.tracker.domain.health

import com.example.tracker.core.TimeProvider
import com.example.tracker.domain.model.CustomStat
import com.example.tracker.domain.model.StatEntry
import com.example.tracker.domain.model.StatIcon
import com.example.tracker.domain.progress.bodyweightStat
import com.example.tracker.domain.repository.CustomStatRepository
import com.example.tracker.domain.repository.SettingsRepository
import java.time.Duration
import java.time.LocalDate
import kotlin.math.round
import kotlinx.coroutines.flow.first

/** What the app reads from the phone's health data, each into a stat of its own. */
enum class HealthMetric { STEPS, WEIGHT, SLEEP }

/** One day's total (steps), last value (weight, kg) or sleep (hours). */
data class DailyValue(val date: LocalDate, val value: Double)

/** The phone's health data store, such as Health Connect. */
interface HealthDataSource {
    /** Whether it exists on this phone and can be used, so the app can offer it. */
    fun availability(): HealthAvailability

    /** The metrics the user lets the app read. */
    suspend fun grantedMetrics(): Set<HealthMetric>

    /** Values per day from [from] to [to], both included, in the phone's time zone. */
    suspend fun readDaily(metric: HealthMetric, from: LocalDate, to: LocalDate): List<DailyValue>
}

enum class HealthAvailability {
    AVAILABLE,

    /** Supported, but the Health Connect app has to be installed or updated first. */
    NEEDS_INSTALL,

    /** This phone can't have it (Android 8.1 and older). */
    UNSUPPORTED,
}

/**
 * Copies the last [DAYS] days of steps, weight and sleep from [source] into the matching
 * stats, making any that is missing from [newStat]. A day that has health data gets it; days
 * without keep whatever the user logged. Only metrics the user allowed are read.
 */
class HealthSync(
    private val source: HealthDataSource,
    private val stats: CustomStatRepository,
    private val settings: SettingsRepository,
    private val time: TimeProvider,
    /** A new stat for [HealthMetric], named in the user's language. */
    private val newStat: (HealthMetric) -> CustomStat,
) {
    /** Syncs when the user turned it on and the last sync is older than [MIN_INTERVAL]. */
    suspend fun syncIfDue() {
        if (!settings.healthSyncEnabled.value) return
        val last = settings.lastHealthSync.value
        if (last != null && Duration.between(last, time.now()) < MIN_INTERVAL) return
        sync()
    }

    /** Syncs now; returns how many days of data were written. Throws if reading fails. */
    suspend fun sync(): Int {
        if (source.availability() != HealthAvailability.AVAILABLE) return 0
        val granted = source.grantedMetrics()
        val today = time.now().atZone(time.zone()).toLocalDate()
        val from = today.minusDays(DAYS - 1)
        var written = 0
        val existing = stats.observeOverviewsOnce()
        for (metric in HealthMetric.entries.filter { it in granted }) {
            val values = source.readDaily(metric, from, today).filter { it.value > 0.0 }
            if (values.isEmpty()) continue
            val stat = existing.forMetric(metric) ?: newStat(metric).let { it.copy(id = stats.createStat(it)) }
            values.forEach { day ->
                stats.saveEntry(StatEntry(stat.id, day.date, day.value.roundTo(stat.decimals)))
                written++
            }
        }
        settings.setLastHealthSync(time.now())
        return written
    }

    private suspend fun CustomStatRepository.observeOverviewsOnce(): List<CustomStat> =
        observeOverviews().first().map { it.stat }

    companion object {
        /** How far back each sync reads, today included. */
        const val DAYS = 30L

        /** Opening the app more often than this doesn't sync again. */
        val MIN_INTERVAL: Duration = Duration.ofMinutes(15)

        private const val DECIMAL_BASE = 10

        /** The stat [metric] goes into: Steps, Sleep, or the [bodyweightStat] that × BW uses too. */
        fun List<CustomStat>.forMetric(metric: HealthMetric): CustomStat? = when (metric) {
            HealthMetric.STEPS -> firstOrNull { it.icon == StatIcon.STEPS }
            HealthMetric.SLEEP -> firstOrNull { it.icon == StatIcon.SLEEP }
            HealthMetric.WEIGHT -> bodyweightStat()
        }

        private fun Double.roundTo(decimals: Int): Double {
            var factor = 1.0
            repeat(decimals) { factor *= DECIMAL_BASE }
            return round(this * factor) / factor
        }
    }
}
