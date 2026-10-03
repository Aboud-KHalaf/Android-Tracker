package com.example.tracker.domain.repository

import com.example.tracker.domain.model.WeightEntry
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

/**
 * Body weight, at most one entry per day. Write functions throw [IllegalArgumentException] for
 * out-of-range values; storage failures propagate as exceptions.
 */
interface WeightRepository {
    /** Entries from [from] to [to], both inclusive, newest first. */
    fun observeEntries(from: LocalDate, to: LocalDate): Flow<List<WeightEntry>>

    /** The most recent entry ever logged, or null when there is none. */
    fun observeLatest(): Flow<WeightEntry?>

    fun observeEntry(date: LocalDate): Flow<WeightEntry?>

    /** Logs [entry], replacing anything already logged for that date. */
    suspend fun saveEntry(entry: WeightEntry)

    suspend fun deleteEntry(date: LocalDate)
}
