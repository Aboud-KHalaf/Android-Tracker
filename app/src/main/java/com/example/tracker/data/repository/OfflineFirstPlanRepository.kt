package com.example.tracker.data.repository

import androidx.room.withTransaction
import com.example.tracker.core.IdGenerator
import com.example.tracker.core.TimeProvider
import com.example.tracker.data.local.SyncMetadata
import com.example.tracker.data.local.TrackerDatabase
import com.example.tracker.data.local.entity.PlanEntity
import com.example.tracker.data.local.entity.PlanExerciseEntity
import com.example.tracker.domain.model.ExerciseConfig
import com.example.tracker.domain.model.PlanDetails
import com.example.tracker.domain.model.WorkoutPlan
import com.example.tracker.domain.repository.PlanRepository
import com.example.tracker.domain.workout.Supersets
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
            plan?.let {
                PlanDetails(
                    id = it.id,
                    name = it.name,
                    exercises = exercises.map { row -> row.toDomain() },
                    dayOfWeek = it.dayOfWeek?.toDayOfWeek(),
                )
            }
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

    override suspend fun addExercise(planId: String, exerciseId: String, config: ExerciseConfig): String {
        requireValid(config)
        return database.withTransaction {
            requirePlan(planId)
            val exercise = requireNotNull(exerciseDao.get(exerciseId)) { "Exercise $exerciseId not found" }
            val id = ids.newId()
            val entity = PlanExerciseEntity(
                id = id,
                planId = planId,
                exerciseId = exerciseId,
                position = planDao.maxPosition(planId) + 1,
                targetSets = 0,
                sync = SyncMetadata.created(now()),
            ).withConfig(config.forType(exercise.type))
            planDao.upsertPlanExercises(listOf(entity))
            id
        }
    }

    override suspend fun replaceExercise(planExerciseId: String, exerciseId: String) {
        database.withTransaction {
            val row = requireActivePlanExercise(planExerciseId)
            if (row.exerciseId == exerciseId) return@withTransaction
            val exercise = requireNotNull(exerciseDao.get(exerciseId)) { "Exercise $exerciseId not found" }
            val config = row.config().forType(exercise.type)
            planDao.upsertPlanExercises(
                listOf(row.copy(exerciseId = exerciseId, sync = row.sync.touched(now())).withConfig(config)),
            )
        }
    }

    override suspend fun updateExerciseConfig(planExerciseId: String, config: ExerciseConfig) {
        requireValid(config)
        database.withTransaction {
            val row = requireActivePlanExercise(planExerciseId)
            val type = requireNotNull(exerciseDao.get(row.exerciseId)) { "Exercise ${row.exerciseId} not found" }.type
            planDao.upsertPlanExercises(listOf(row.copy(sync = row.sync.touched(now())).withConfig(config.forType(type))))
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

    override suspend fun supersetWithNext(planExerciseId: String) {
        database.withTransaction {
            val row = requireActivePlanExercise(planExerciseId)
            val ordered = planDao.getPlanExercises(row.planId)
            val index = ordered.indexOfFirst { it.id == planExerciseId }
            val next = ordered.getOrNull(index + 1) ?: return@withTransaction
            val groups = ordered.map { it.supersetGroup }
            val group = row.supersetGroup ?: next.supersetGroup ?: ids.newId()
            // Both runs (this exercise's and the next one's) become one superset.
            val members = Supersets.runOf(groups, index) + Supersets.runOf(groups, index + 1)
            val now = now()
            planDao.upsertPlanExercises(
                members.map { ordered[it] }
                    .filter { it.supersetGroup != group }
                    .map { it.copy(supersetGroup = group, sync = it.sync.touched(now)) },
            )
        }
    }

    override suspend fun removeFromSuperset(planExerciseId: String) {
        database.withTransaction {
            val row = requireActivePlanExercise(planExerciseId)
            if (row.supersetGroup == null) return@withTransaction
            val ordered = planDao.getPlanExercises(row.planId)
            val index = ordered.indexOfFirst { it.id == planExerciseId }
            val run = Supersets.runOf(ordered.map { it.supersetGroup }, index)
            val now = now()
            // Taking out a middle exercise splits the rest in two, so the later part gets its
            // own group; any part left with one exercise leaves the superset too.
            val before = (run.first until index).map { ordered[it] }
            val after = (index + 1..run.last).map { ordered[it] }
            val afterGroup = if (after.size > 1) ids.newId() else null
            val changed = buildList {
                add(row.copy(supersetGroup = null, sync = row.sync.touched(now)))
                if (before.size == 1) add(before.single().let { it.copy(supersetGroup = null, sync = it.sync.touched(now)) })
                after.forEach { add(it.copy(supersetGroup = afterGroup, sync = it.sync.touched(now))) }
            }
            planDao.upsertPlanExercises(changed)
        }
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

    private suspend fun requireActivePlanExercise(id: String): PlanExerciseEntity =
        requirePlanExercise(id).also { require(it.sync.deletedAt == null) { "Plan exercise $id was removed" } }

    private fun requireValid(config: ExerciseConfig) {
        require(config.targetSets in 1..ExerciseConfig.MAX_TARGET_SETS) { "Sets must be 1–${ExerciseConfig.MAX_TARGET_SETS}" }
        require(config.targetReps == null || config.targetReps >= 1) { "Target reps must be at least 1" }
        require(config.targetWeightKg == null || config.targetWeightKg >= 0.0) { "Target weight must not be negative" }
        require(config.targetDurationSeconds == null || config.targetDurationSeconds >= 1) {
            "Target hold must be at least one second"
        }
    }
}

private fun PlanExerciseEntity.config() = ExerciseConfig(
    targetSets = targetSets,
    targetReps = targetReps,
    targetWeightKg = targetWeightKg,
    targetDurationSeconds = targetDurationSeconds,
)

private fun PlanExerciseEntity.withConfig(config: ExerciseConfig) = copy(
    targetSets = config.targetSets,
    targetReps = config.targetReps,
    targetWeightKg = config.targetWeightKg,
    targetDurationSeconds = config.targetDurationSeconds,
)
