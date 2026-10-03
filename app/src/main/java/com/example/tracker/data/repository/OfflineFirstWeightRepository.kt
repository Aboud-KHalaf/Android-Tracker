package com.example.tracker.data.repository

import com.example.tracker.core.TimeProvider
import com.example.tracker.data.local.SyncMetadata
import com.example.tracker.data.local.TrackerDatabase
import com.example.tracker.data.local.entity.BodyWeightEntity
import com.example.tracker.domain.model.WeightEntry
import com.example.tracker.domain.repository.WeightRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Reads and writes the local database; every write is marked pending for a future sync. */
class OfflineFirstWeightRepository(
    database: TrackerDatabase,
    private val time: TimeProvider,
) : WeightRepository {

    private val weightDao = database.weightDao()

    override fun observeEntries(from: LocalDate, to: LocalDate): Flow<List<WeightEntry>> =
        weightDao.observeRange(from.toEpochDay(), to.toEpochDay()).map { rows -> rows.map { it.toDomain() } }

    override fun observeLatest(): Flow<WeightEntry?> = weightDao.observeLatest().map { it?.toDomain() }

    override fun observeEntry(date: LocalDate): Flow<WeightEntry?> =
        weightDao.observe(date.toEpochDay()).map { it?.toDomain() }

    override suspend fun saveEntry(entry: WeightEntry) {
        require(entry.weightKg in WeightEntry.MIN_KG..WeightEntry.MAX_KG) { "Weight out of range: ${entry.weightKg}" }
        val now = time.now().toEpochMilli()
        val existing = weightDao.getIncludingDeleted(entry.date.toEpochDay())
        val sync = when {
            existing == null -> SyncMetadata.created(now)
            existing.sync.deletedAt != null -> existing.sync.restored(now)
            else -> existing.sync.touched(now)
        }
        weightDao.upsert(listOf(BodyWeightEntity(entry.date.toEpochDay(), entry.weightKg, sync)))
    }

    override suspend fun deleteEntry(date: LocalDate) {
        val existing = weightDao.getIncludingDeleted(date.toEpochDay())
        if (existing == null || existing.sync.deletedAt != null) return
        weightDao.upsert(listOf(existing.copy(sync = existing.sync.deleted(time.now().toEpochMilli()))))
    }
}
