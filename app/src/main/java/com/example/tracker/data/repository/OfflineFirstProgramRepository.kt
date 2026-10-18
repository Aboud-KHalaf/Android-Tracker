package com.example.tracker.data.repository

import androidx.room.withTransaction
import com.example.tracker.core.IdGenerator
import com.example.tracker.core.TimeProvider
import com.example.tracker.data.local.SyncMetadata
import com.example.tracker.data.local.TrackerDatabase
import com.example.tracker.data.local.entity.PlanEntity
import com.example.tracker.data.local.entity.ProgramEntity
import com.example.tracker.domain.model.Program
import com.example.tracker.domain.model.WorkoutPlan
import com.example.tracker.domain.repository.ProgramRepository
import java.time.DayOfWeek
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/** Reads and writes the local database; every write is marked pending for a future sync. */
class OfflineFirstProgramRepository(
    private val database: TrackerDatabase,
    private val time: TimeProvider,
    private val ids: IdGenerator,
) : ProgramRepository {

    private val programDao = database.programDao()
    private val planDao = database.planDao()

    override fun observePrograms(): Flow<List<Program>> =
        combine(programDao.observeAll(), planDao.observeSummaries()) { programs, plans ->
            val daysByProgram = plans.map { it.toDomain() }.groupBy { it.programId }
            programs.map { it.toDomain(daysByProgram[it.id].orEmpty()) }
        }

    override fun observeProgram(id: String): Flow<Program?> =
        observePrograms().map { programs -> programs.firstOrNull { it.id == id } }

    override suspend fun createProgram(name: String, schedule: Map<DayOfWeek, String>): String {
        val programName = requireName(name)
        val dayNames = schedule.mapValues { (_, dayName) -> requireName(dayName) }
        return database.withTransaction {
            val now = now()
            val id = ids.newId()
            val isFirst = programDao.getAll().none { it.isActive }
            programDao.upsert(listOf(ProgramEntity(id, programName, isActive = isFirst, SyncMetadata.created(now))))
            planDao.upsertPlans(
                dayNames.entries.sortedBy { it.key }.map { (day, dayName) ->
                    PlanEntity(ids.newId(), dayName, programId = id, dayOfWeek = day.value, SyncMetadata.created(now))
                },
            )
            id
        }
    }

    override suspend fun renameProgram(id: String, name: String) {
        val program = requireProgram(id)
        programDao.upsert(listOf(program.copy(name = requireName(name), sync = program.sync.touched(now()))))
    }

    override suspend fun deleteProgram(id: String) {
        database.withTransaction {
            val program = requireProgram(id)
            val now = now()
            programDao.softDeleteExercisesOfProgram(id, now)
            programDao.softDeleteDaysOfProgram(id, now)
            programDao.upsert(listOf(program.copy(isActive = false, sync = program.sync.deleted(now))))
            if (program.isActive) {
                // Keep one program active: the oldest remaining one takes over.
                programDao.getAll().firstOrNull()?.let { next ->
                    programDao.upsert(listOf(next.copy(isActive = true, sync = next.sync.touched(now))))
                }
            }
        }
    }

    override suspend fun setActiveProgram(id: String) {
        database.withTransaction {
            requireProgram(id)
            val now = now()
            val changed = programDao.getAll().mapNotNull { program ->
                val active = program.id == id
                if (program.isActive == active) null else program.copy(isActive = active, sync = program.sync.touched(now))
            }
            programDao.upsert(changed)
        }
    }

    override suspend fun addDay(programId: String, dayOfWeek: DayOfWeek?, name: String): String {
        val dayName = requireName(name)
        return database.withTransaction {
            requireProgram(programId)
            if (dayOfWeek != null) requireFree(programId, dayOfWeek)
            val id = ids.newId()
            planDao.upsertPlans(
                listOf(PlanEntity(id, dayName, programId, dayOfWeek?.value, SyncMetadata.created(now()))),
            )
            id
        }
    }

    override suspend fun scheduleDay(planId: String, dayOfWeek: DayOfWeek?) {
        database.withTransaction {
            val plan = requireNotNull(planDao.get(planId)) { "Day $planId not found" }
            if (plan.dayOfWeek == dayOfWeek?.value) return@withTransaction
            val programId = requireNotNull(plan.programId) { "Day $planId has no program" }
            if (dayOfWeek != null) requireFree(programId, dayOfWeek)
            planDao.upsertPlans(listOf(plan.copy(dayOfWeek = dayOfWeek?.value, sync = plan.sync.touched(now()))))
        }
    }

    private suspend fun requireFree(programId: String, dayOfWeek: DayOfWeek) {
        require(planDao.getDayOn(programId, dayOfWeek.value) == null) { "$dayOfWeek already has a training day" }
    }

    private fun now(): Long = time.now().toEpochMilli()

    private suspend fun requireProgram(id: String): ProgramEntity =
        requireNotNull(programDao.get(id)) { "Program $id not found" }
}

/** Scheduled days Monday to Sunday, then unscheduled ones in the order they were added. */
private fun ProgramEntity.toDomain(days: List<WorkoutPlan>) = Program(
    id = id,
    name = name,
    isActive = isActive,
    days = days.sortedWith(compareBy(nullsLast()) { it.dayOfWeek }),
)
