package com.example.tracker.domain.progress

import com.example.tracker.domain.model.ExerciseSession
import com.example.tracker.domain.model.ExerciseType
import java.time.Instant
import java.time.ZoneId

/** A per-session number to chart an exercise's progress by. */
enum class ProgressMetric {
    /** Heaviest set's weight, kg. */
    WEIGHT,

    /** Reps over all sets. */
    REPS,

    /** Weight × reps over all sets, kg. */
    VOLUME,

    /** Longest hold, seconds. */
    HOLD;

    /** This metric's value for [session], or null if the session has none. */
    fun valueOf(session: ExerciseSession): Double? {
        val stats = session.stats()
        return when (this) {
            WEIGHT -> stats.topWeightKg
            REPS -> stats.totalReps.takeIf { it > 0 }?.toDouble()
            VOLUME -> stats.volumeKg.takeIf { it > 0 }
            HOLD -> stats.longestHoldSeconds?.toDouble()
        }
    }

    companion object {
        /** The metrics that apply to an exercise type, the default first. */
        fun forType(type: ExerciseType): List<ProgressMetric> = when (type) {
            ExerciseType.WEIGHT_REPS -> listOf(WEIGHT, REPS, VOLUME)
            ExerciseType.DURATION -> listOf(HOLD)
        }
    }
}

/** How far back the progress chart looks. */
enum class TimeRange(private val months: Long?) {
    ONE_MONTH(1),
    THREE_MONTHS(3),
    ONE_YEAR(12),
    ALL(null);

    /** Whether something that happened at [at] is in this range, counting back from [now]. */
    fun contains(at: Instant, now: Instant, zone: ZoneId): Boolean {
        val months = months ?: return true
        val start = now.atZone(zone).minusMonths(months).toInstant()
        return !at.isBefore(start)
    }
}
