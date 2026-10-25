package com.example.tracker.domain.repository

import com.example.tracker.domain.model.CustomStat
import com.example.tracker.domain.model.StatEntry
import com.example.tracker.domain.model.StatOverview
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

/**
 * The user's own stats and their daily values, at most one per stat and day. Write functions
 * throw [IllegalArgumentException] for invalid input; storage failures propagate as exceptions.
 */
interface CustomStatRepository {
    /** Every stat in the user's order (oldest first until reordered), with its first and latest entries. */
    fun observeOverviews(): Flow<List<StatOverview>>

    /** Entries of [statId] from [from] to [to], both inclusive, newest first. */
    fun observeEntries(statId: String, from: LocalDate, to: LocalDate): Flow<List<StatEntry>>

    fun observeEntry(statId: String, date: LocalDate): Flow<StatEntry?>

    /** The stat, or null when there is none with [id] (or it was deleted). */
    suspend fun getStat(id: String): CustomStat?

    /** Creates a stat from [stat], ignoring its id; returns the new id. */
    suspend fun createStat(stat: CustomStat): String

    suspend fun updateStat(stat: CustomStat)

    /** Puts the stats in the order of [statIds], which must list every stat once. */
    suspend fun reorderStats(statIds: List<String>)

    /** Deletes the stat; its entries go with it. */
    suspend fun deleteStat(id: String)

    /** Logs [entry], replacing anything already logged for that stat and date. */
    suspend fun saveEntry(entry: StatEntry)

    suspend fun deleteEntry(statId: String, date: LocalDate)
}
