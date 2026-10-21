package com.example.tracker.domain.workout

import com.example.tracker.domain.model.Workout

/** How long to rest after a set, in seconds; 0 turns that rest off. */
data class RestTimes(
    val betweenSetsSeconds: Int = DEFAULT_BETWEEN_SETS_SECONDS,
    val betweenExercisesSeconds: Int = DEFAULT_BETWEEN_EXERCISES_SECONDS,
) {
    /** Whether any rest is timed, so its end may need an alert. */
    val isAnyOn: Boolean get() = betweenSetsSeconds > 0 || betweenExercisesSeconds > 0

    fun secondsFor(kind: RestKind): Int = when (kind) {
        RestKind.BETWEEN_SETS -> betweenSetsSeconds
        RestKind.BETWEEN_EXERCISES -> betweenExercisesSeconds
    }

    companion object {
        const val DEFAULT_BETWEEN_SETS_SECONDS = 90
        const val DEFAULT_BETWEEN_EXERCISES_SECONDS = 120

        /** The rest times offered in Settings; 0 is "Off". */
        val OPTIONS: List<Int> = listOf(0, 30, 45, 60, 90, 120, 150, 180, 240, 300)
    }
}

/** Whether a rest leads to another set of the same exercise or to the next exercise. */
enum class RestKind { BETWEEN_SETS, BETWEEN_EXERCISES }

/** A rest to take: [kind] for [seconds]. */
data class RestPeriod(val kind: RestKind, val seconds: Int)

/**
 * The rest after completing [setId]: between sets while its exercise has sets left, between
 * exercises while another exercise does. Null when the workout has nothing left to do, the
 * set isn't in this workout, or that rest is turned off.
 */
fun Workout.restAfterCompleting(setId: String, times: RestTimes): RestPeriod? {
    val exercise = exercises.firstOrNull { exercise -> exercise.sets.any { it.id == setId } } ?: return null
    val kind = when {
        exercise.sets.any { it.id != setId && !it.isCompleted } -> RestKind.BETWEEN_SETS
        exercises.any { other -> other.id != exercise.id && other.sets.any { !it.isCompleted } } -> RestKind.BETWEEN_EXERCISES
        else -> return null
    }
    val seconds = times.secondsFor(kind)
    return if (seconds > 0) RestPeriod(kind, seconds) else null
}

/** What follows a completed set: the exercise to show (null to stay put) and the rest, if any. */
data class AfterSet(val exerciseIndex: Int?, val rest: RestPeriod?)

/**
 * What follows completing [setId], taking supersets into account. Within a superset the next
 * exercise of the round follows at once, without rest; after the round's last exercise comes
 * the rest between sets and the first exercise with sets left. Once the superset is done, or
 * outside one, this is [restAfterCompleting] on the same exercise. Indices are in exercise
 * order.
 */
fun Workout.afterCompleting(setId: String, times: RestTimes): AfterSet {
    val ordered = exercises.sortedBy { it.position }
    val index = ordered.indexOfFirst { exercise -> exercise.sets.any { it.id == setId } }
    if (index < 0) return AfterSet(null, null)
    val run = Supersets.runOf(ordered.map { it.supersetGroup }, index)
    if (run.count() > 1) {
        fun hasSetsLeft(i: Int) = ordered[i].sets.any { it.id != setId && !it.isCompleted }
        (index + 1..run.last).firstOrNull(::hasSetsLeft)?.let { return AfterSet(it, null) }
        (run.first..index).firstOrNull(::hasSetsLeft)?.let { next ->
            val seconds = times.secondsFor(RestKind.BETWEEN_SETS)
            return AfterSet(next, if (seconds > 0) RestPeriod(RestKind.BETWEEN_SETS, seconds) else null)
        }
    }
    return AfterSet(null, restAfterCompleting(setId, times))
}
