package com.example.tracker.data.repository

import androidx.room.withTransaction
import com.example.tracker.core.IdGenerator
import com.example.tracker.core.TimeProvider
import com.example.tracker.data.local.SyncMetadata
import com.example.tracker.data.local.TrackerDatabase
import com.example.tracker.data.local.entity.ExerciseEntity
import com.example.tracker.domain.model.Exercise
import com.example.tracker.domain.model.ExerciseType
import com.example.tracker.domain.repository.ExerciseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Reads and writes the local database; every write is marked pending for a future sync. */
class OfflineFirstExerciseRepository(
    private val database: TrackerDatabase,
    private val time: TimeProvider,
    private val ids: IdGenerator,
) : ExerciseRepository {

    private val exerciseDao = database.exerciseDao()
    private val planDao = database.planDao()

    override fun observeExercises(): Flow<List<Exercise>> =
        exerciseDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observeExercise(id: String): Flow<Exercise?> =
        exerciseDao.observe(id).map { it?.toDomain() }

    override suspend fun createExercise(name: String, type: ExerciseType): String {
        val id = ids.newId()
        val now = time.now().toEpochMilli()
        exerciseDao.upsert(listOf(ExerciseEntity(id, requireName(name), type, SyncMetadata.created(now))))
        return id
    }

    override suspend fun renameExercise(id: String, name: String) {
        val exercise = requireExercise(id)
        val now = time.now().toEpochMilli()
        exerciseDao.upsert(listOf(exercise.copy(name = requireName(name), sync = exercise.sync.touched(now))))
    }

    override suspend fun deleteExercise(id: String) {
        database.withTransaction {
            val exercise = requireExercise(id)
            val now = time.now().toEpochMilli()
            exerciseDao.upsert(listOf(exercise.copy(sync = exercise.sync.deleted(now))))
            planDao.softDeleteByExercise(id, now)
        }
    }

    private suspend fun requireExercise(id: String): ExerciseEntity =
        requireNotNull(exerciseDao.get(id)) { "Exercise $id not found" }
}

internal fun requireName(name: String): String {
    val trimmed = name.trim()
    require(trimmed.isNotEmpty()) { "Name must not be blank" }
    return trimmed
}
