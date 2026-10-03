package com.example.tracker.domain.weight

import com.example.tracker.domain.model.WeightEntry

/** How weight moved over a period's entries. */
data class WeightSummary(
    val first: WeightEntry,
    val latest: WeightEntry,
    val lowestKg: Double,
    val highestKg: Double,
    val entryCount: Int,
) {
    /** Latest minus first, in kg; negative means weight went down. Zero with a single entry. */
    val changeKg: Double get() = latest.weightKg - first.weightKg

    companion object {
        /** Null when there are no entries. */
        fun of(entries: List<WeightEntry>): WeightSummary? {
            if (entries.isEmpty()) return null
            val byDate = entries.sortedBy { it.date }
            return WeightSummary(
                first = byDate.first(),
                latest = byDate.last(),
                lowestKg = entries.minOf { it.weightKg },
                highestKg = entries.maxOf { it.weightKg },
                entryCount = entries.size,
            )
        }
    }
}
