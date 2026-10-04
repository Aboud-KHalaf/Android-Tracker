package com.example.tracker.data.repository

import com.example.tracker.core.TimeProvider
import com.example.tracker.data.local.SyncMetadata
import com.example.tracker.data.local.TrackerDatabase
import com.example.tracker.data.local.entity.DailyNutritionEntity
import com.example.tracker.domain.model.DailyNutrition
import com.example.tracker.domain.repository.NutritionRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Reads and writes the local database; every write is marked pending for a future sync. */
class OfflineFirstNutritionRepository(
    database: TrackerDatabase,
    private val time: TimeProvider,
) : NutritionRepository {

    private val nutritionDao = database.nutritionDao()

    override fun observeDays(from: LocalDate, to: LocalDate): Flow<List<DailyNutrition>> =
        nutritionDao.observeRange(from.toEpochDay(), to.toEpochDay()).map { rows -> rows.map { it.toDomain() } }

    override fun observeDay(date: LocalDate): Flow<DailyNutrition?> =
        nutritionDao.observe(date.toEpochDay()).map { it?.toDomain() }

    override suspend fun saveDay(day: DailyNutrition) {
        require(day.calories in 0..DailyNutrition.MAX_CALORIES) { "Calories out of range: ${day.calories}" }
        require(day.proteinGrams in 0..DailyNutrition.MAX_PROTEIN_GRAMS) { "Protein out of range: ${day.proteinGrams}" }
        require(day.steps == null || day.steps in 0..DailyNutrition.MAX_STEPS) { "Steps out of range: ${day.steps}" }
        val now = time.now().toEpochMilli()
        val existing = nutritionDao.getIncludingDeleted(day.date.toEpochDay())
        val sync = when {
            existing == null -> SyncMetadata.created(now)
            existing.sync.deletedAt != null -> existing.sync.restored(now)
            else -> existing.sync.touched(now)
        }
        nutritionDao.upsert(listOf(DailyNutritionEntity(day.date.toEpochDay(), day.calories, day.proteinGrams, day.steps, sync)))
    }

    override suspend fun deleteDay(date: LocalDate) {
        val existing = nutritionDao.getIncludingDeleted(date.toEpochDay())
        if (existing == null || existing.sync.deletedAt != null) return
        nutritionDao.upsert(listOf(existing.copy(sync = existing.sync.deleted(time.now().toEpochMilli()))))
    }
}
