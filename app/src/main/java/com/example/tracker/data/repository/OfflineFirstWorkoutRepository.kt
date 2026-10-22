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
import com.example.tracker.domain.model.Rpe
import com.example.tracker.domain.model.SetType
import com.example.tracker.domain.model.WeekSummary
import com.example.tracker.domain.model.Workout
import com.example.tracker.domain.model.WorkoutNote
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

    override fun observeAllSessions(): Flow<List<ExerciseSession>> =
        workoutDao.observeLoggedSets(exerciseId = null).map { rows -> rows.map { it.toDomain() }.toSessions() }

    override fun observePersonalBests(): Flow<List<PersonalBest>> =
        observeAllSessions().map { findPersonalBests(it) }

    override suspend fun lastTimeSets(exerciseId: String): List<WorkoutSet> =
        workoutDao.lastCompletedSets(exerciseId).map { it.toDomain() }

    override suspend fun lastTimeNote(exerciseId: String): String? = workoutDao.lastExerciseNote(exerciseId)

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
                supersetGroup = planExercise.supersetGroup,
                sync = SyncMetadata.created(now),
            )
            val lastTime = workoutDao.lastCompletedSets(planExercise.exerciseId)
            repeat(planExercise.targetSets) { position ->
                // Prefill from the configured targets; a target left open repeats the same set
                // last time, or the last set if there were fewer.
                val template = lastTime.getOrNull(position) ?: lastTime.lastOrNull()
                sets += WorkoutSetEntity(
                    id = ids.newId(),
                    workoutExerciseId = workoutExerciseId,
                    position = position,
                    weightKg = planExercise.targetWeightKg ?: template?.weightKg,
                    reps = planExercise.targetReps ?: template?.reps,
                    durationSeconds = planExercise.targetDurationSeconds ?: template?.durationSeconds,
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

    override suspend fun setWorkoutNote(workoutId: String, note: String?) {
        val normalized = WorkoutNote.normalize(note)
        val workout = requireWorkout(workoutId)
        if (workout.note == normalized) return
        workoutDao.upsertWorkouts(listOf(workout.copy(note = normalized, sync = workout.sync.touched(now()))))
    }

    override suspend fun setExerciseNote(workoutExerciseId: String, note: String?) {
        val normalized = WorkoutNote.normalize(note)
        val exercise = requireNotNull(workoutDao.getWorkoutExercise(workoutExerciseId)) {
            "Workout exercise $workoutExerciseId not found"
        }
        if (exercise.note == normalized) return
        workoutDao.upsertWorkoutExercises(listOf(exercise.copy(note = normalized, sync = exercise.sync.touched(now()))))
    }

    override suspend fun setCurrentExercise(workoutId: String, exerciseIndex: Int) {
        require(exerciseIndex >= 0) { "Exercise index must not be negative" }
        val workout = requireWorkout(workoutId)
        if (workout.currentExercisePosition == exerciseIndex) return
        workoutDao.upsertWorkouts(
            listOf(workout.copy(currentExercisePosition = exerciseIndex, sync = workout.sync.touched(now()))),
        )
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

    override suspend fun addSet(workoutExerciseId: String, type: SetType): String = database.withTransaction {
        requireNotNull(workoutDao.getWorkoutExercise(workoutExerciseId)) {
            "Workout exercise $workoutExerciseId not found"
        }
        val sets = workoutDao.getSets(workoutExerciseId)
        val now = now()
        val previous: WorkoutSetEntity?
        val position: Int
        if (type == SetType.WARM_UP) {
            previous = sets.lastOrNull { it.type == SetType.WARM_UP }
            position = previous?.position?.plus(1) ?: sets.firstOrNull()?.position ?: 0
            workoutDao.shiftSets(workoutExerciseId, from = position, now = now)
        } else {
            previous = sets.lastOrNull()
            position = workoutDao.maxSetPosition(workoutExerciseId) + 1
        }
        val id = ids.newId()
        val set = WorkoutSetEntity(
            id = id,
            workoutExerciseId = workoutExerciseId,
            position = position,
            weightKg = previous?.weightKg,
            reps = previous?.reps,
            // A new timed set starts from zero rather than copying the last hold.
            durationSeconds = null,
            completedAt = null,
            type = type,
            sync = SyncMetadata.created(now),
        )
        workoutDao.upsertSets(listOf(set))
        id
    }

    override suspend fun setSetType(setId: String, type: SetType) {
        val set = requireSet(setId)
        if (set.type == type) return
        workoutDao.upsertSets(listOf(set.copy(type = type, sync = set.sync.touched(now()))))
    }

    override suspend fun setSetRpe(setId: String, rpe: Double?) {
        require(rpe == null || rpe in Rpe.MIN..Rpe.MAX) { "RPE must be between ${Rpe.MIN} and ${Rpe.MAX}" }
        val set = requireSet(setId)
        if (set.rpe == rpe) return
        workoutDao.upsertSets(listOf(set.copy(rpe = rpe, sync = set.sync.touched(now()))))
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
