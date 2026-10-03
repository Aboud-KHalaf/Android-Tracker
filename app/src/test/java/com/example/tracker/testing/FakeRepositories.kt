package com.example.tracker.testing

import com.example.tracker.domain.model.Exercise
import com.example.tracker.domain.model.ExerciseSession
import com.example.tracker.domain.model.ExerciseType
import com.example.tracker.domain.model.PersonalBest
import com.example.tracker.domain.model.PlanDetails
import com.example.tracker.domain.model.WeekSummary
import com.example.tracker.domain.model.Workout
import com.example.tracker.domain.model.WorkoutPlan
import com.example.tracker.domain.model.WorkoutSet
import com.example.tracker.domain.model.WorkoutSummary
import com.example.tracker.domain.repository.ExerciseRepository
import com.example.tracker.domain.repository.PlanRepository
import com.example.tracker.domain.repository.WorkoutRepository
import java.time.Duration
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
    override suspend fun renameExercise(id: String, name: String) = TODO("Not used yet")
    override suspend fun deleteExercise(id: String) = TODO("Not used yet")
}

class FakeWorkoutRepository : WorkoutRepository {
    val activeWorkout = MutableStateFlow<Workout?>(null)
    val history = MutableStateFlow<List<WorkoutSummary>>(emptyList())
    val weekSummary = MutableStateFlow(WeekSummary(0, Duration.ZERO, emptySet()))
    val personalBests = MutableStateFlow<List<PersonalBest>>(emptyList())

    /** When set, [observeHistory] fails with it, so screens can show their error state. */
    var historyError: Exception? = null
    var startWorkoutError: Exception? = null
    val startedPlanIds = mutableListOf<String>()
    var requestedWeekStart: LocalDate? = null
        private set

    override fun observeActiveWorkout(): Flow<Workout?> = activeWorkout
    override fun observeWorkout(id: String): Flow<Workout?> = TODO("Not used yet")

    override fun observeHistory(planId: String?): Flow<List<WorkoutSummary>> = flow {
        historyError?.let { throw it }
        emitAll(history.map { all -> all.filter { planId == null || it.planId == planId } })
    }

    override fun observeWeekSummary(weekStart: LocalDate): Flow<WeekSummary> {
        requestedWeekStart = weekStart
        return weekSummary
    }

    override fun observeExerciseSessions(exerciseId: String): Flow<List<ExerciseSession>> = TODO("Not used yet")
    override fun observePersonalBests(): Flow<List<PersonalBest>> = personalBests
    override suspend fun lastTimeSets(exerciseId: String): List<WorkoutSet> = TODO("Not used yet")

    override suspend fun startWorkout(planId: String): String {
        startWorkoutError?.let { throw it }
        startedPlanIds += planId
        return "workout-${startedPlanIds.size}"
    }

    override suspend fun finishWorkout(workoutId: String) = TODO("Not used yet")
    override suspend fun discardWorkout(workoutId: String) = TODO("Not used yet")
    override suspend fun addSet(workoutExerciseId: String): String = TODO("Not used yet")
    override suspend fun updateSet(setId: String, weightKg: Double?, reps: Int?, durationSeconds: Int?) = TODO("Not used yet")
    override suspend fun completeSet(setId: String) = TODO("Not used yet")
    override suspend fun reopenSet(setId: String) = TODO("Not used yet")
    override suspend fun deleteSet(setId: String) = TODO("Not used yet")
}
