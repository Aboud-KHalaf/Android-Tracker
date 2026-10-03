package com.example.tracker.data.repository

import androidx.room.withTransaction
import com.example.tracker.core.IdGenerator
import com.example.tracker.core.TimeProvider
import com.example.tracker.data.local.SyncMetadata
import com.example.tracker.data.local.TrackerDatabase
import com.example.tracker.data.local.entity.WorkoutEntity
import com.example.tracker.data.local.entity.WorkoutExerciseEntity
import com.example.tracker.data.local.entity.WorkoutSetEntity
import com.example.tracker.domain.model.ExerciseSession
import com.example.tracker.domain.model.PersonalBest
import com.example.tracker.domain.model.WeekSummary
import com.example.tracker.domain.model.Workout
import com.example.tracker.domain.model.WorkoutSet
import com.example.tracker.domain.model.WorkoutSummary
import com.example.tracker.domain.progress.findPersonalBests
import com.example.tracker.domain.progress.summarizeWeek
import com.example.tracker.domain.progress.toSessions
import com.example.tracker.domain.repository.WorkoutRepository
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** Reads and writes the local database; every write is marked pending for a future sync. */
class OfflineFirstWorkoutRepository(
    private val database: TrackerDatabase,
    private val time: TimeProvider,
    private val ids: IdGenerator,
) : WorkoutRepository {

    private val workoutDao = database.workoutDao()
    private val planDao = database.planDao()

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeActiveWorkout(): Flow<Workout?> =
        workoutDao.observeActive()
            .map { it?.id }
            .distinctUntilChanged()
            .flatMapLatest { id -> if (id == null) flowOf(null) else observeWorkout(id) }

    override fun observeWorkout(id: String): Flow<Workout?> =
        combine(
            workoutDao.observe(id),
            workoutDao.observeExercises(id),
            workoutDao.observeSets(id),
        ) { workout, exercises, sets -> workout?.toDomain(exercises, sets) }

    override fun observeHistory(planId: String?): Flow<List<WorkoutSummary>> =
        combine(
            workoutDao.observeFinishedSummaries(planId),
            observePersonalBests(),
        ) { rows, personalBests ->
            val countByWorkout = personalBests.groupingBy { it.workoutId }.eachCount()
            rows.map { it.toDomain(personalBestCount = countByWorkout[it.id] ?: 0) }
        }

    override fun observeWeekSummary(weekStart: LocalDate): Flow<WeekSummary> =
        observeHistory().map { summarizeWeek(it, weekStart, time.zone()) }

    override fun observeExerciseSessions(exerciseId: String): Flow<List<ExerciseSession>> =
        workoutDao.observeLoggedSets(exerciseId).map { rows -> rows.map { it.toDomain() }.toSessions() }

    override fun observePersonalBests(): Flow<List<PersonalBest>> =
        workoutDao.observeLoggedSets(exerciseId = null)
            .map { rows -> findPersonalBests(rows.map { it.toDomain() }.toSessions()) }

    override suspend fun lastTimeSets(exerciseId: String): List<WorkoutSet> =
        workoutDao.lastCompletedSets(exerciseId).map { it.toDomain() }

    override suspend fun startWorkout(planId: String): String = database.withTransaction {
        check(workoutDao.getActive() == null) { "A workout is already in progress" }
        val plan = requireNotNull(planDao.get(planId)) { "Plan $planId not found" }
        val now = now()
        val workoutId = ids.newId()
        val exercises = mutableListOf<WorkoutExerciseEntity>()
        val sets = mutableListOf<WorkoutSetEntity>()

        planDao.getPlanExercises(planId).forEachIndexed { index, planExercise ->
            val workoutExerciseId = ids.newId()
            exercises += WorkoutExerciseEntity(
                id = workoutExerciseId,
                workoutId = workoutId,
                exerciseId = planExercise.exerciseId,
                position = index,
                sync = SyncMetadata.created(now),
            )
            val lastTime = workoutDao.lastCompletedSets(planExercise.exerciseId)
            repeat(planExercise.targetSets) { position ->
                // Prefill from the same set last time, or the last set if there were fewer.
                val template = lastTime.getOrNull(position) ?: lastTime.lastOrNull()
                sets += WorkoutSetEntity(
                    id = ids.newId(),
                    workoutExerciseId = workoutExerciseId,
                    position = position,
                    weightKg = template?.weightKg,
                    reps = template?.reps,
                    durationSeconds = template?.durationSeconds,
                    completedAt = null,
                    sync = SyncMetadata.created(now),
                )
            }
        }

        val workout = WorkoutEntity(
            id = workoutId,
            planId = plan.id,
            name = plan.name,
            startedAt = now,
            finishedAt = null,
            sync = SyncMetadata.created(now),
        )
        workoutDao.upsertWorkouts(listOf(workout))
        workoutDao.upsertWorkoutExercises(exercises)
        workoutDao.upsertSets(sets)
        workoutId
    }

    override suspend fun finishWorkout(workoutId: String) {
        val workout = requireWorkout(workoutId)
        check(workout.finishedAt == null) { "Workout $workoutId is already finished" }
        val now = now()
        workoutDao.upsertWorkouts(listOf(workout.copy(finishedAt = now, sync = workout.sync.touched(now))))
    }

    override suspend fun discardWorkout(workoutId: String) {
        database.withTransaction {
            val workout = requireWorkout(workoutId)
            val now = now()
            workoutDao.softDeleteSetsOfWorkout(workoutId, now)
            workoutDao.softDeleteExercisesOfWorkout(workoutId, now)
            workoutDao.upsertWorkouts(listOf(workout.copy(sync = workout.sync.deleted(now))))
        }
    }

    override suspend fun addSet(workoutExerciseId: String): String = database.withTransaction {
        requireNotNull(workoutDao.getWorkoutExercise(workoutExerciseId)) {
            "Workout exercise $workoutExerciseId not found"
        }
        val previous = workoutDao.getSets(workoutExerciseId).lastOrNull()
        val id = ids.newId()
        val set = WorkoutSetEntity(
            id = id,
            workoutExerciseId = workoutExerciseId,
            position = workoutDao.maxSetPosition(workoutExerciseId) + 1,
            weightKg = previous?.weightKg,
            reps = previous?.reps,
            // A new timed set starts from zero rather than copying the last hold.
            durationSeconds = null,
            completedAt = null,
            sync = SyncMetadata.created(now()),
        )
        workoutDao.upsertSets(listOf(set))
        id
    }

    override suspend fun updateSet(setId: String, weightKg: Double?, reps: Int?, durationSeconds: Int?) {
        require(weightKg == null || weightKg >= 0.0) { "Weight must not be negative" }
        require(reps == null || reps >= 0) { "Reps must not be negative" }
        require(durationSeconds == null || durationSeconds >= 0) { "Duration must not be negative" }
        val set = requireSet(setId)
        workoutDao.upsertSets(
            listOf(
                set.copy(
                    weightKg = weightKg,
                    reps = reps,
                    durationSeconds = durationSeconds,
                    sync = set.sync.touched(now()),
                )
            )
        )
    }

    override suspend fun completeSet(setId: String) {
        val set = requireSet(setId)
        val now = now()
        workoutDao.upsertSets(listOf(set.copy(completedAt = now, sync = set.sync.touched(now))))
    }

    override suspend fun reopenSet(setId: String) {
        val set = requireSet(setId)
        workoutDao.upsertSets(listOf(set.copy(completedAt = null, sync = set.sync.touched(now()))))
    }

    override suspend fun deleteSet(setId: String) {
        val set = requireSet(setId)
        workoutDao.upsertSets(listOf(set.copy(sync = set.sync.deleted(now()))))
    }

    private fun now(): Long = time.now().toEpochMilli()

    private suspend fun requireWorkout(id: String): WorkoutEntity =
        requireNotNull(workoutDao.get(id)) { "Workout $id not found" }

    private suspend fun requireSet(id: String): WorkoutSetEntity =
        requireNotNull(workoutDao.getSet(id)) { "Set $id not found" }
}
