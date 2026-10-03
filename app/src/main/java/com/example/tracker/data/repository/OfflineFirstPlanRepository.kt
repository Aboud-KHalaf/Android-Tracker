package com.example.tracker.data.repository

import androidx.room.withTransaction
import com.example.tracker.core.IdGenerator
import com.example.tracker.core.TimeProvider
import com.example.tracker.data.local.SyncMetadata
import com.example.tracker.data.local.TrackerDatabase
import com.example.tracker.data.local.entity.PlanEntity
import com.example.tracker.data.local.entity.PlanExerciseEntity
import com.example.tracker.domain.model.PlanDetails
import com.example.tracker.domain.model.WorkoutPlan
import com.example.tracker.domain.repository.PlanRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/** Reads and writes the local database; every write is marked pending for a future sync. */
class OfflineFirstPlanRepository(
    private val database: TrackerDatabase,
    private val time: TimeProvider,
    private val ids: IdGenerator,
) : PlanRepository {

    private val planDao = database.planDao()
    private val exerciseDao = database.exerciseDao()

    override fun observePlans(): Flow<List<WorkoutPlan>> =
        planDao.observeSummaries().map { rows -> rows.map { it.toDomain() } }

    override fun observePlan(id: String): Flow<PlanDetails?> =
        combine(planDao.observe(id), planDao.observeExercises(id)) { plan, exercises ->
            plan?.let { PlanDetails(it.id, it.name, exercises.map { row -> row.toDomain() }) }
        }

    override suspend fun createPlan(name: String): String {
        val id = ids.newId()
        val now = now()
        planDao.upsertPlans(listOf(PlanEntity(id, requireName(name), SyncMetadata.created(now))))
        return id
    }

    override suspend fun renamePlan(id: String, name: String) {
        val plan = requirePlan(id)
        planDao.upsertPlans(listOf(plan.copy(name = requireName(name), sync = plan.sync.touched(now()))))
    }

    override suspend fun deletePlan(id: String) {
        database.withTransaction {
            val plan = requirePlan(id)
            val now = now()
            planDao.upsertPlans(listOf(plan.copy(sync = plan.sync.deleted(now))))
            planDao.softDeleteExercisesOfPlan(id, now)
        }
    }

    override suspend fun addExercise(planId: String, exerciseId: String, targetSets: Int): String {
        require(targetSets >= 1) { "A plan exercise needs at least one set" }
        return database.withTransaction {
            requirePlan(planId)
            requireNotNull(exerciseDao.get(exerciseId)) { "Exercise $exerciseId not found" }
            val id = ids.newId()
            val entity = PlanExerciseEntity(
                id = id,
                planId = planId,
                exerciseId = exerciseId,
                position = planDao.maxPosition(planId) + 1,
                targetSets = targetSets,
                sync = SyncMetadata.created(now()),
            )
            planDao.upsertPlanExercises(listOf(entity))
            id
        }
    }

    override suspend fun removeExercise(planExerciseId: String) {
        val row = requirePlanExercise(planExerciseId)
        if (row.sync.deletedAt != null) return
        planDao.upsertPlanExercises(listOf(row.copy(sync = row.sync.deleted(now()))))
    }

    override suspend fun restoreExercise(planExerciseId: String) {
        val row = requirePlanExercise(planExerciseId)
        if (row.sync.deletedAt == null) return
        planDao.upsertPlanExercises(listOf(row.copy(sync = row.sync.restored(now()))))
    }

    override suspend fun moveExercise(planId: String, fromIndex: Int, toIndex: Int) {
        database.withTransaction {
            val ordered = planDao.getPlanExercises(planId).toMutableList()
            require(fromIndex in ordered.indices && toIndex in ordered.indices) {
                "Move $fromIndex → $toIndex is out of range for ${ordered.size} exercises"
            }
            ordered.add(toIndex, ordered.removeAt(fromIndex))
            val now = now()
            // Rewrite positions as 0..n-1 and only touch rows whose position changed.
            val changed = ordered.mapIndexedNotNull { index, row ->
                if (row.position == index) null else row.copy(position = index, sync = row.sync.touched(now))
            }
            planDao.upsertPlanExercises(changed)
        }
    }

    private fun now(): Long = time.now().toEpochMilli()

    private suspend fun requirePlan(id: String): PlanEntity =
        requireNotNull(planDao.get(id)) { "Plan $id not found" }

    private suspend fun requirePlanExercise(id: String): PlanExerciseEntity =
        requireNotNull(planDao.getPlanExerciseIncludingDeleted(id)) { "Plan exercise $id not found" }
}
