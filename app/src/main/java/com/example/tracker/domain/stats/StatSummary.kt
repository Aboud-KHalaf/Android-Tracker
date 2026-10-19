package com.example.tracker.domain.stats

import com.example.tracker.domain.model.StatEntry

/** How a custom stat moved over a period's entries. */
data class StatSummary(
    val first: StatEntry,
    val latest: StatEntry,
    val lowest: Double,
    val highest: Double,
    val entryCount: Int,
) {
    /** Latest minus first; zero with a single entry. */
    val change: Double get() = latest.value - first.value

    companion object {
        /** Null when there are no entries. */
        fun of(entries: List<StatEntry>): StatSummary? {
            if (entries.isEmpty()) return null
            val byDate = entries.sortedBy { it.date }
            return StatSummary(
                first = byDate.first(),
                latest = byDate.last(),
                lowest = entries.minOf { it.value },
                highest = entries.maxOf { it.value },
                entryCount = entries.size,
            )
        }
    }
}
