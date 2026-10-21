package com.example.tracker.domain.workout

/**
 * Supersets are exercises done in turn, one set each, before resting. Each exercise carries an
 * optional superset group; a superset is a run of neighbouring exercises with the same group.
 * Exercises with the same group that aren't next to each other (e.g. after a reorder) are
 * separate runs, and a run of one is just an exercise.
 */
object Supersets {
    /** The indices of the superset that the exercise at [index] belongs to; just [index] when it is in none. */
    fun runOf(groups: List<String?>, index: Int): IntRange {
        val group = groups.getOrNull(index) ?: return index..index
        var first = index
        while (first > 0 && groups[first - 1] == group) first--
        var last = index
        while (last < groups.lastIndex && groups[last + 1] == group) last++
        return first..last
    }

    /** Every superset of two or more exercises, in order. */
    fun runs(groups: List<String?>): List<IntRange> {
        val runs = mutableListOf<IntRange>()
        var index = 0
        while (index < groups.size) {
            val run = runOf(groups, index)
            if (run.count() > 1) runs += run
            index = run.last + 1
        }
        return runs
    }
}
