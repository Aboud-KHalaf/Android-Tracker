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
    HOLD,

    /** Heaviest set's weight as a multiple of bodyweight that day. */
    RELATIVE_STRENGTH;

    /**
     * This metric's value for [session], or null if the session has none. [bodyweightKg] is
     * the user's bodyweight that day, needed for [RELATIVE_STRENGTH].
     */
    fun valueOf(session: ExerciseSession, bodyweightKg: Double? = null): Double? {
        val stats = session.stats()
        return when (this) {
            WEIGHT -> stats.topWeightKg
            REPS -> stats.totalReps.takeIf { it > 0 }?.toDouble()
            VOLUME -> stats.volumeKg.takeIf { it > 0 }
            HOLD -> stats.longestHoldSeconds?.toDouble()
            RELATIVE_STRENGTH -> stats.topWeightKg?.takeIf { it > 0.0 }?.let { top ->
                bodyweightKg?.takeIf { it > 0.0 }?.let { top / it }
            }
        }
    }

    companion object {
        /**
         * The metrics that apply to an exercise type, the default first. Relative strength
         * needs bodyweight, so it is offered only [withBodyweight].
         */
        fun forType(type: ExerciseType, withBodyweight: Boolean = false): List<ProgressMetric> = when (type) {
            ExerciseType.WEIGHT_REPS -> listOf(WEIGHT, REPS, VOLUME) + if (withBodyweight) listOf(RELATIVE_STRENGTH) else emptyList()
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
