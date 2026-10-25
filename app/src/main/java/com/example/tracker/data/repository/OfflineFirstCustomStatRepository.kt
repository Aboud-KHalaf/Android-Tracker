package com.example.tracker.data.repository

import androidx.room.withTransaction
import com.example.tracker.core.IdGenerator
import com.example.tracker.core.TimeProvider
import com.example.tracker.data.local.SyncMetadata
import com.example.tracker.data.local.TrackerDatabase
import com.example.tracker.data.local.entity.CustomStatEntity
import com.example.tracker.data.local.entity.CustomStatEntryEntity
import com.example.tracker.domain.model.CustomStat
import com.example.tracker.domain.model.StatColor
import com.example.tracker.domain.model.StatGoal
import com.example.tracker.domain.model.StatEntry
import com.example.tracker.domain.model.StatIcon
import com.example.tracker.domain.model.StatOverview
import com.example.tracker.domain.repository.CustomStatRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * Reads and writes the local database; every write is marked pending for a future sync.
 * Deleting a stat leaves a tombstone on the stat only: its entries are hidden with it.
 */
class OfflineFirstCustomStatRepository(
    private val database: TrackerDatabase,
    private val time: TimeProvider,
    private val ids: IdGenerator,
) : CustomStatRepository {

    private val dao = database.customStatDao()

    override fun observeOverviews(): Flow<List<StatOverview>> =
        combine(dao.observeStats(), dao.observeFirstAndLatestEntries()) { stats, edges ->
            val edgesByStat = edges.groupBy { it.statId }
            stats.map { row ->
                val entries = edgesByStat[row.id].orEmpty()
                StatOverview(
                    stat = row.toDomain(),
                    first = entries.minByOrNull { it.epochDay }?.toDomain(),
                    latest = entries.maxByOrNull { it.epochDay }?.toDomain(),
                )
            }
        }

    override fun observeEntries(statId: String, from: LocalDate, to: LocalDate): Flow<List<StatEntry>> =
        dao.observeEntries(statId, from.toEpochDay(), to.toEpochDay()).map { rows -> rows.map { it.toDomain() } }

    override fun observeEntry(statId: String, date: LocalDate): Flow<StatEntry?> =
        dao.observeEntry(statId, date.toEpochDay()).map { it?.toDomain() }

    override suspend fun getStat(id: String): CustomStat? =
        dao.getStatIncludingDeleted(id)?.takeIf { it.sync.deletedAt == null }?.toDomain()

    override suspend fun createStat(stat: CustomStat): String {
        validate(stat)
        return database.withTransaction {
            val id = ids.newId()
            // New stats go last.
            val position = dao.maxPosition() + 1
            dao.upsertStat(stat.copy(id = id).toEntity(SyncMetadata.created(time.now().toEpochMilli()), position))
            id
        }
    }

    override suspend fun updateStat(stat: CustomStat) {
        validate(stat)
        val existing = dao.getStatIncludingDeleted(stat.id)
        require(existing != null && existing.sync.deletedAt == null) { "No stat ${stat.id}" }
        dao.upsertStat(stat.toEntity(existing.sync.touched(time.now().toEpochMilli()), existing.position))
    }

    override suspend fun reorderStats(statIds: List<String>) {
        database.withTransaction {
            val stats = dao.getStats()
            require(statIds.toSet() == stats.mapTo(mutableSetOf()) { it.id } && statIds.size == stats.size) {
                "Order must list every stat once: $statIds"
            }
            val now = time.now().toEpochMilli()
            val byId = stats.associateBy { it.id }
            val changed = statIds.mapIndexedNotNull { index, id ->
                val stat = byId.getValue(id)
                if (stat.position == index) null else stat.copy(position = index, sync = stat.sync.touched(now))
            }
            dao.upsertStats(changed)
        }
    }

    override suspend fun deleteStat(id: String) {
        val existing = dao.getStatIncludingDeleted(id)
        if (existing == null || existing.sync.deletedAt != null) return
        dao.upsertStat(existing.copy(sync = existing.sync.deleted(time.now().toEpochMilli())))
    }

    override suspend fun saveEntry(entry: StatEntry) {
        require(entry.value.isFinite() && entry.value in CustomStat.MIN_VALUE..CustomStat.MAX_VALUE) {
            "Value out of range: ${entry.value}"
        }
        requireNotNull(getStat(entry.statId)) { "No stat ${entry.statId}" }
        val now = time.now().toEpochMilli()
        val existing = dao.getEntryIncludingDeleted(entry.statId, entry.date.toEpochDay())
        val sync = when {
            existing == null -> SyncMetadata.created(now)
            existing.sync.deletedAt != null -> existing.sync.restored(now)
            else -> existing.sync.touched(now)
        }
        dao.upsertEntry(CustomStatEntryEntity(entry.statId, entry.date.toEpochDay(), entry.value, sync))
    }

    override suspend fun deleteEntry(statId: String, date: LocalDate) {
        val existing = dao.getEntryIncludingDeleted(statId, date.toEpochDay())
        if (existing == null || existing.sync.deletedAt != null) return
        dao.upsertEntry(existing.copy(sync = existing.sync.deleted(time.now().toEpochMilli())))
    }

    private fun validate(stat: CustomStat) {
        require(stat.name.isNotBlank() && stat.name.length <= CustomStat.MAX_NAME_LENGTH) { "Invalid name: ${stat.name}" }
        require(stat.unit.length <= CustomStat.MAX_UNIT_LENGTH) { "Invalid unit: ${stat.unit}" }
        require(stat.decimals in CustomStat.DECIMALS_RANGE) { "Invalid decimals: ${stat.decimals}" }
        stat.target?.let { require(it.isFinite() && it in CustomStat.MIN_VALUE..CustomStat.MAX_VALUE) { "Invalid target: $it" } }
    }

    private fun CustomStat.toEntity(sync: SyncMetadata, position: Int) = CustomStatEntity(
        id = id,
        name = name.trim(),
        unit = unit.trim(),
        decimals = decimals,
        target = target,
        icon = icon.name,
        color = color.name,
        sync = sync,
        goal = goal.name,
        position = position,
    )

    private fun CustomStatEntity.toDomain() = CustomStat(
        id = id,
        name = name,
        unit = unit,
        decimals = decimals,
        target = target,
        icon = StatIcon.entries.firstOrNull { it.name == icon } ?: StatIcon.CHART,
        color = StatColor.entries.firstOrNull { it.name == color } ?: StatColor.BLUE,
        goal = StatGoal.entries.firstOrNull { it.name == goal } ?: StatGoal.CHANGE,
    )

    private fun CustomStatEntryEntity.toDomain() = StatEntry(statId, LocalDate.ofEpochDay(epochDay), value)
}
