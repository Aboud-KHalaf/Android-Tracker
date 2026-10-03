package com.example.tracker.testing

import com.example.tracker.domain.model.Exercise
import com.example.tracker.domain.model.ExerciseSession
import com.example.tracker.domain.model.ExerciseType
import com.example.tracker.domain.model.NutritionTargets
import com.example.tracker.domain.model.PersonalBest
import com.example.tracker.domain.model.PlanDetails
import com.example.tracker.domain.model.ThemeMode
import com.example.tracker.domain.model.WeekSummary
import com.example.tracker.domain.model.Workout
import com.example.tracker.domain.model.WorkoutPlan
import com.example.tracker.domain.model.WorkoutSet
import com.example.tracker.domain.model.WorkoutSummary
import com.example.tracker.domain.repository.ExerciseRepository
import com.example.tracker.domain.repository.PlanRepository
import com.example.tracker.domain.repository.SettingsRepository
import com.example.tracker.domain.repository.WorkoutRepository
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/*
 * In-memory repositories for ViewModel tests. Each exposes its data as mutable state and
 * implements what screens use so far; extend them as new screens need more.
 */

class FakePlanRepository : PlanRepository {
    val plans = MutableStateFlow<List<WorkoutPlan>>(emptyList())
    val planDetails = MutableStateFlow<Map<String, PlanDetails>>(emptyMap())

    /** When set, every write fails with it. */
    var writeError: Exception? = null
    var createPlanError: Exception? = null
    val createdPlanNames = mutableListOf<String>()

    /** Writes in the order they happened, e.g. "remove pe1", "move push 0->1". */
    val writes = mutableListOf<String>()

    override fun observePlans(): Flow<List<WorkoutPlan>> = plans
    override fun observePlan(id: String): Flow<PlanDetails?> = planDetails.map { it[id] }

    override suspend fun createPlan(name: String): String {
        createPlanError?.let { throw it }
        createdPlanNames += name
        return "plan-${createdPlanNames.size}"
    }

    override suspend fun renamePlan(id: String, name: String) = write("rename $id $name")
    override suspend fun deletePlan(id: String) = write("delete $id")

    override suspend fun addExercise(planId: String, exerciseId: String, targetSets: Int): String {
        write("add $planId $exerciseId")
        return "pe-$exerciseId"
    }

    override suspend fun removeExercise(planExerciseId: String) = write("remove $planExerciseId")
    override suspend fun restoreExercise(planExerciseId: String) = write("restore $planExerciseId")
    override suspend fun moveExercise(planId: String, fromIndex: Int, toIndex: Int) =
        write("move $planId $fromIndex->$toIndex")

    private fun write(description: String) {
        writeError?.let { throw it }
        writes += description
    }
}

class FakeExerciseRepository : ExerciseRepository {
    val exercises = MutableStateFlow<List<Exercise>>(emptyList())

    override fun observeExercises(): Flow<List<Exercise>> = exercises
    override fun observeExercise(id: String): Flow<Exercise?> = exercises.map { all -> all.firstOrNull { it.id == id } }
    val createdExercises = mutableListOf<Pair<String, ExerciseType>>()

    override suspend fun createExercise(name: String, type: ExerciseType): String {
        createdExercises += name to type
        return "exercise-${createdExercises.size}"
    }
    var renameError: Exception? = null

    override suspend fun renameExercise(id: String, name: String) {
        renameError?.let { throw it }
        exercises.value = exercises.value.map { if (it.id == id) it.copy(name = name) else it }
    }
    override suspend fun deleteExercise(id: String) = TODO("Not used yet")
}

class FakeWorkoutRepository(private val now: () -> Instant = { Instant.parse("2026-10-03T09:00:00Z") }) :
    WorkoutRepository {
    /** The workout store: the one workout these tests work with, in progress or finished. */
    val activeWorkout = MutableStateFlow<Workout?>(null)
    val history = MutableStateFlow<List<WorkoutSummary>>(emptyList())
    val weekSummary = MutableStateFlow(WeekSummary(0, Duration.ZERO, emptySet()))
    val personalBests = MutableStateFlow<List<PersonalBest>>(emptyList())

    /** Last-time sets by exercise id. */
    val lastTimes = mutableMapOf<String, List<WorkoutSet>>()

    /** When set, [observeHistory] fails with it, so screens can show their error state. */
    var historyError: Exception? = null
    var startWorkoutError: Exception? = null

    /** When set, every set or workout write fails with it. */
    var writeError: Exception? = null
    val startedPlanIds = mutableListOf<String>()
    var requestedWeekStart: LocalDate? = null
        private set
    private var nextSetId = 1

    override fun observeActiveWorkout(): Flow<Workout?> = activeWorkout.map { it?.takeIf { w -> w.isInProgress } }
    override fun observeWorkout(id: String): Flow<Workout?> = activeWorkout.map { it?.takeIf { w -> w.id == id } }

    override fun observeHistory(planId: String?): Flow<List<WorkoutSummary>> = flow {
        historyError?.let { throw it }
        emitAll(history.map { all -> all.filter { planId == null || it.planId == planId } })
    }

    override fun observeWeekSummary(weekStart: LocalDate): Flow<WeekSummary> {
        requestedWeekStart = weekStart
        return weekSummary
    }

    /** Completed sessions by exercise id. */
    val sessions = MutableStateFlow<Map<String, List<ExerciseSession>>>(emptyMap())

    override fun observeExerciseSessions(exerciseId: String): Flow<List<ExerciseSession>> =
        sessions.map { it[exerciseId].orEmpty() }
    override fun observePersonalBests(): Flow<List<PersonalBest>> = personalBests
    override suspend fun lastTimeSets(exerciseId: String): List<WorkoutSet> = lastTimes[exerciseId].orEmpty()

    override suspend fun startWorkout(planId: String): String {
        startWorkoutError?.let { throw it }
        startedPlanIds += planId
        return "workout-${startedPlanIds.size}"
    }

    override suspend fun finishWorkout(workoutId: String) = writeWorkout { it.copy(finishedAt = now()) }

    override suspend fun discardWorkout(workoutId: String) {
        writeError?.let { throw it }
        activeWorkout.value = null
    }

    override suspend fun addSet(workoutExerciseId: String): String {
        val id = "new-set-${nextSetId++}"
        writeWorkout { workout ->
            workout.copy(
                exercises = workout.exercises.map { exercise ->
                    if (exercise.id != workoutExerciseId) return@map exercise
                    val last = exercise.sets.maxByOrNull { it.position }
                    val set = WorkoutSet(id, (last?.position ?: -1) + 1, last?.weightKg, last?.reps, null, null)
                    exercise.copy(sets = exercise.sets + set)
                },
            )
        }
        return id
    }

    override suspend fun updateSet(setId: String, weightKg: Double?, reps: Int?, durationSeconds: Int?) =
        writeSet(setId) { it.copy(weightKg = weightKg, reps = reps, durationSeconds = durationSeconds) }

    override suspend fun completeSet(setId: String) = writeSet(setId) { it.copy(completedAt = now()) }
    override suspend fun reopenSet(setId: String) = writeSet(setId) { it.copy(completedAt = null) }

    override suspend fun deleteSet(setId: String) = writeWorkout { workout ->
        workout.copy(exercises = workout.exercises.map { e -> e.copy(sets = e.sets.filterNot { it.id == setId }) })
    }

    /** The set with [setId] in the stored workout. */
    fun set(setId: String): WorkoutSet = activeWorkout.value!!.exercises.flatMap { it.sets }.first { it.id == setId }

    private fun writeSet(setId: String, change: (WorkoutSet) -> WorkoutSet) = writeWorkout { workout ->
        workout.copy(
            exercises = workout.exercises.map { e -> e.copy(sets = e.sets.map { if (it.id == setId) change(it) else it }) },
        )
    }

    private fun writeWorkout(change: (Workout) -> Workout) {
        writeError?.let { throw it }
        activeWorkout.value = change(requireNotNull(activeWorkout.value) { "No workout" })
    }
}

class FakeSettingsRepository : SettingsRepository {
    override val themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    override val nutritionTargets = MutableStateFlow(NutritionTargets())

    /** When set, every write fails with it. */
    var writeError: Exception? = null
    var deleteAllDataCalls = 0

    override suspend fun setThemeMode(mode: ThemeMode) {
        writeError?.let { throw it }
        themeMode.value = mode
    }

    override suspend fun setNutritionTargets(targets: NutritionTargets) {
        writeError?.let { throw it }
        nutritionTargets.value = targets
    }

    override suspend fun deleteAllData() {
        writeError?.let { throw it }
        deleteAllDataCalls++
    }
}
