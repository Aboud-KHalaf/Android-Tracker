package com.example.tracker.testing

import com.example.tracker.domain.model.CatalogExercise
import com.example.tracker.domain.model.CustomStat
import com.example.tracker.domain.model.Exercise
import com.example.tracker.domain.model.ExerciseConfig
import com.example.tracker.domain.model.ExerciseSession
import com.example.tracker.domain.model.ExerciseType
import com.example.tracker.domain.model.MuscleGroup
import com.example.tracker.domain.model.PersonalBest
import com.example.tracker.domain.model.PhotoMeasurement
import com.example.tracker.domain.model.PhotoPose
import com.example.tracker.domain.model.PlanDetails
import com.example.tracker.domain.model.Program
import com.example.tracker.domain.model.ProgressPhoto
import com.example.tracker.domain.model.StatEntry
import com.example.tracker.domain.model.StatOverview
import com.example.tracker.domain.model.ThemeMode
import com.example.tracker.domain.model.WeekSummary
import com.example.tracker.domain.model.Workout
import com.example.tracker.domain.model.WorkoutPlan
import com.example.tracker.domain.model.SetType
import com.example.tracker.domain.model.WorkoutNote
import com.example.tracker.domain.model.WorkoutSet
import com.example.tracker.domain.model.WorkoutSummary
import com.example.tracker.domain.photos.PhotoCheckIn
import com.example.tracker.domain.reminder.ReminderTime
import com.example.tracker.domain.repository.CustomStatRepository
import com.example.tracker.domain.repository.ExerciseCatalogRepository
import com.example.tracker.domain.repository.ExerciseRepository
import com.example.tracker.domain.repository.PlanRepository
import com.example.tracker.domain.repository.ProgramRepository
import com.example.tracker.domain.repository.ProgressPhotoRepository
import com.example.tracker.domain.repository.SettingsRepository
import com.example.tracker.domain.repository.WorkoutRepository
import com.example.tracker.domain.workout.RestAlarm
import com.example.tracker.domain.workout.RestKind
import com.example.tracker.domain.workout.RestTimes
import java.io.File
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
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

    /** Writes in the order they happened, e.g. "remove pe1", "move push 0->1". */
    val writes = mutableListOf<String>()

    /** Saved configurations by plan exercise id. */
    val savedConfigs = mutableMapOf<String, ExerciseConfig>()

    override fun observePlans(): Flow<List<WorkoutPlan>> = plans
    override fun observePlan(id: String): Flow<PlanDetails?> = planDetails.map { it[id] }

    override suspend fun renamePlan(id: String, name: String) = write("rename $id $name")
    override suspend fun deletePlan(id: String) = write("delete $id")

    override suspend fun addExercise(planId: String, exerciseId: String, config: ExerciseConfig): String {
        write("add $planId $exerciseId")
        return "pe-$exerciseId"
    }

    override suspend fun replaceExercise(planExerciseId: String, exerciseId: String) =
        write("replace $planExerciseId $exerciseId")

    override suspend fun updateExerciseConfig(planExerciseId: String, config: ExerciseConfig) {
        write("config $planExerciseId")
        savedConfigs[planExerciseId] = config
    }

    override suspend fun removeExercise(planExerciseId: String) = write("remove $planExerciseId")
    override suspend fun restoreExercise(planExerciseId: String) = write("restore $planExerciseId")
    override suspend fun moveExercise(planId: String, fromIndex: Int, toIndex: Int) =
        write("move $planId $fromIndex->$toIndex")

    override suspend fun supersetWithNext(planExerciseId: String) = write("superset $planExerciseId")

    override suspend fun removeFromSuperset(planExerciseId: String) = write("unsuperset $planExerciseId")

    private fun write(description: String) {
        writeError?.let { throw it }
        writes += description
    }
}

class FakeProgramRepository : ProgramRepository {
    val programs = MutableStateFlow<List<Program>>(emptyList())

    /** When set, every write fails with it. */
    var writeError: Exception? = null

    /** Writes in the order they happened, e.g. "active p1", "schedule d1 MONDAY". */
    val writes = mutableListOf<String>()

    /** Programs created so far, with their schedules. */
    val created = mutableListOf<Pair<String, Map<DayOfWeek, String>>>()

    override fun observePrograms(): Flow<List<Program>> = programs
    override fun observeProgram(id: String): Flow<Program?> = programs.map { all -> all.firstOrNull { it.id == id } }

    override suspend fun createProgram(name: String, schedule: Map<DayOfWeek, String>): String {
        writeError?.let { throw it }
        created += name to schedule
        return "program-${created.size}"
    }

    override suspend fun renameProgram(id: String, name: String) = write("rename $id $name")
    override suspend fun deleteProgram(id: String) = write("delete $id")
    override suspend fun setActiveProgram(id: String) = write("active $id")

    override suspend fun addDay(programId: String, dayOfWeek: DayOfWeek?, name: String): String {
        write("add $programId $dayOfWeek $name")
        return "day-${writes.size}"
    }

    override suspend fun scheduleDay(planId: String, dayOfWeek: DayOfWeek?) = write("schedule $planId $dayOfWeek")

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

    /** The muscle group each created exercise was given, in order. */
    val createdMuscleGroups = mutableListOf<MuscleGroup?>()

    override suspend fun createExercise(name: String, type: ExerciseType, muscleGroup: MuscleGroup?): String {
        createdExercises += name to type
        createdMuscleGroups += muscleGroup
        return "exercise-${createdExercises.size}"
    }

    override suspend fun setMuscleGroup(id: String, muscleGroup: MuscleGroup?) {
        exercises.value = exercises.value.map { if (it.id == id) it.copy(muscleGroup = muscleGroup) else it }
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

    override fun observeAllSessions(): Flow<List<ExerciseSession>> =
        sessions.map { all -> all.values.flatten().sortedBy { it.finishedAt } }
    override fun observePersonalBests(): Flow<List<PersonalBest>> = personalBests
    override suspend fun lastTimeSets(exerciseId: String): List<WorkoutSet> = lastTimes[exerciseId].orEmpty()

    /** Last time's note per exercise id. */
    val lastNotes = mutableMapOf<String, String>()

    override suspend fun lastTimeNote(exerciseId: String): String? = lastNotes[exerciseId]

    override suspend fun setWorkoutNote(workoutId: String, note: String?) =
        writeWorkout { it.copy(note = WorkoutNote.normalize(note)) }

    override suspend fun setExerciseNote(workoutExerciseId: String, note: String?) = writeWorkout { workout ->
        workout.copy(
            exercises = workout.exercises.map {
                if (it.id == workoutExerciseId) it.copy(note = WorkoutNote.normalize(note)) else it
            },
        )
    }

    override suspend fun startWorkout(planId: String): String {
        startWorkoutError?.let { throw it }
        startedPlanIds += planId
        return "workout-${startedPlanIds.size}"
    }

    override suspend fun finishWorkout(workoutId: String) = writeWorkout { it.copy(finishedAt = now()) }

    override suspend fun setCurrentExercise(workoutId: String, exerciseIndex: Int) =
        writeWorkout { it.copy(currentExerciseIndex = exerciseIndex) }

    override suspend fun discardWorkout(workoutId: String) {
        writeError?.let { throw it }
        activeWorkout.value = null
    }

    override suspend fun addSet(workoutExerciseId: String, type: SetType): String {
        val id = "new-set-${nextSetId++}"
        writeWorkout { workout ->
            workout.copy(
                exercises = workout.exercises.map { exercise ->
                    if (exercise.id != workoutExerciseId) return@map exercise
                    val ordered = exercise.sets.sortedBy { it.position }
                    if (type == SetType.WARM_UP) {
                        // Warm-ups go before the working sets, which move down one place.
                        val last = ordered.lastOrNull { it.type == SetType.WARM_UP }
                        val position = last?.position?.plus(1) ?: ordered.firstOrNull()?.position ?: 0
                        val set = WorkoutSet(id, position, last?.weightKg, last?.reps, null, null, type)
                        val moved = exercise.sets.map { if (it.position >= position) it.copy(position = it.position + 1) else it }
                        exercise.copy(sets = moved + set)
                    } else {
                        val last = ordered.lastOrNull()
                        val set = WorkoutSet(id, (last?.position ?: -1) + 1, last?.weightKg, last?.reps, null, null, type)
                        exercise.copy(sets = exercise.sets + set)
                    }
                },
            )
        }
        return id
    }

    override suspend fun setSetType(setId: String, type: SetType) = writeSet(setId) { it.copy(type = type) }

    override suspend fun setSetRpe(setId: String, rpe: Double?) = writeSet(setId) { it.copy(rpe = rpe) }

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
    override val healthSyncEnabled = MutableStateFlow(false)
    override val lastHealthSync = MutableStateFlow<Instant?>(null)

    override suspend fun setHealthSyncEnabled(enabled: Boolean) {
        writeError?.let { throw it }
        healthSyncEnabled.value = enabled
    }

    override suspend fun setLastHealthSync(at: Instant) {
        lastHealthSync.value = at
    }

    override val onboardingDone = MutableStateFlow(false)

    override suspend fun setOnboardingDone() {
        writeError?.let { throw it }
        onboardingDone.value = true
    }

    override val themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    override val dynamicColor = MutableStateFlow(false)

    override suspend fun setDynamicColor(enabled: Boolean) {
        writeError?.let { throw it }
        dynamicColor.value = enabled
    }
    override val reminderEnabled = MutableStateFlow(true)
    override val reminderTime = MutableStateFlow(ReminderTime.DEFAULT)
    override val notificationPermissionRequested = MutableStateFlow(false)
    override val restTimes = MutableStateFlow(RestTimes())

    override suspend fun setRestTimes(times: RestTimes) {
        writeError?.let { throw it }
        restTimes.value = times
    }

    override suspend fun setReminderTime(time: LocalTime) {
        writeError?.let { throw it }
        reminderTime.value = time
    }

    override suspend fun setReminderEnabled(enabled: Boolean) {
        writeError?.let { throw it }
        reminderEnabled.value = enabled
    }

    override suspend fun markNotificationPermissionRequested() {
        notificationPermissionRequested.value = true
    }

    /** When set, every write fails with it. */
    var writeError: Exception? = null
    var deleteAllDataCalls = 0

    override suspend fun setThemeMode(mode: ThemeMode) {
        writeError?.let { throw it }
        themeMode.value = mode
    }

    override suspend fun deleteAllData() {
        writeError?.let { throw it }
        deleteAllDataCalls++
    }
}

class FakeProgressPhotoRepository(
    private val capturesDirectory: File = createTempDir(),
    /** When new check-ins are taken; their day is this in UTC. */
    var now: Instant = Instant.parse("2026-10-03T09:00:00Z"),
) : ProgressPhotoRepository {
    /** Every check-in, in any order; [observeCheckIns] sorts them newest first. */
    val checkIns = MutableStateFlow<List<PhotoCheckIn>>(emptyList())

    /** When set, [observeCheckIns] fails with it. */
    var observeError: Exception? = null

    /** When set, every write fails with it. */
    var writeError: Exception? = null
    val discarded = mutableListOf<File>()
    private var nextId = 1

    override fun observeCheckIns(): Flow<List<PhotoCheckIn>> = flow {
        observeError?.let { throw it }
        emitAll(checkIns.map { all -> all.sortedByDescending { it.takenAt } })
    }

    override suspend fun createCaptureFile(): File {
        writeError?.let { throw it }
        return File.createTempFile("capture-", ".jpg", capturesDirectory)
    }

    override suspend fun savePhoto(checkInId: String?, pose: PhotoPose, capture: File): String {
        writeError?.let { throw it }
        val checkIn = checkInId?.let { id -> checkIns.value.first { it.id == id } }
            ?: PhotoCheckIn("check-in-${nextId++}", now.atZone(ZoneOffset.UTC).toLocalDate(), now, emptyMap())
        val photo = ProgressPhoto("photo-${nextId++}", checkIn.id, checkIn.date, pose, capture.absolutePath)
        replace(checkIn.copy(photos = checkIn.photos + (pose to photo)))
        return checkIn.id
    }

    override suspend fun saveMeasurements(checkInId: String, measurements: List<PhotoMeasurement>) {
        writeError?.let { throw it }
        replace(checkIns.value.first { it.id == checkInId }.copy(measurements = measurements))
    }

    override suspend fun discardCapture(capture: File) {
        discarded += capture
        capture.delete()
    }

    override suspend fun deletePhoto(id: String) {
        writeError?.let { throw it }
        checkIns.value = checkIns.value
            .map { checkIn -> checkIn.copy(photos = checkIn.photos.filterValues { it.id != id }) }
            .filter { it.photos.isNotEmpty() }
    }

    override suspend fun deleteCheckIn(id: String) {
        writeError?.let { throw it }
        checkIns.value = checkIns.value.filterNot { it.id == id }
    }

    fun put(vararg taken: PhotoCheckIn) {
        checkIns.value += taken
    }

    private fun replace(checkIn: PhotoCheckIn) {
        checkIns.value = checkIns.value.filterNot { it.id == checkIn.id } + checkIn
    }

    private companion object {
        fun createTempDir(): File = java.nio.file.Files.createTempDirectory("captures").toFile()
    }
}

/** A check-in on [date] at 9:00 UTC plus [hoursLater], with a photo of each of [poses]. */
fun photoCheckIn(
    id: String,
    date: LocalDate,
    vararg poses: PhotoPose,
    hoursLater: Long = 0,
    measurements: List<PhotoMeasurement> = emptyList(),
) = PhotoCheckIn(
    id = id,
    date = date,
    takenAt = date.atTime(9, 0).toInstant(ZoneOffset.UTC).plus(Duration.ofHours(hoursLater)),
    photos = poses.associateWith { ProgressPhoto("$id-$it", id, date, it, "/$id-$it.jpg") },
    measurements = measurements,
)

class FakeCustomStatRepository : CustomStatRepository {
    val stats = MutableStateFlow<List<CustomStat>>(emptyList())

    /** Entries by stat id and date. */
    val entries = MutableStateFlow<Map<Pair<String, LocalDate>, StatEntry>>(emptyMap())

    /** When set, [observeOverviews] fails with it, so screens can show their error state. */
    var observeError: Exception? = null

    /** When set, every write fails with it. */
    var writeError: Exception? = null
    private var nextId = 1

    override fun observeOverviews(): Flow<List<StatOverview>> = flow {
        observeError?.let { throw it }
        emitAll(
            combine(stats, entries) { stats, entries ->
                stats.map { stat ->
                    val ofStat = entries.values.filter { it.statId == stat.id }
                    StatOverview(stat, ofStat.minByOrNull { it.date }, ofStat.maxByOrNull { it.date })
                }
            },
        )
    }

    override fun observeEntries(statId: String, from: LocalDate, to: LocalDate): Flow<List<StatEntry>> =
        entries.map { all -> all.values.filter { it.statId == statId && it.date in from..to }.sortedByDescending { it.date } }

    override fun observeEntry(statId: String, date: LocalDate): Flow<StatEntry?> = entries.map { it[statId to date] }

    override suspend fun getStat(id: String): CustomStat? = stats.value.firstOrNull { it.id == id }

    override suspend fun createStat(stat: CustomStat): String {
        writeError?.let { throw it }
        val id = "stat-${nextId++}"
        stats.value += stat.copy(id = id)
        return id
    }

    override suspend fun updateStat(stat: CustomStat) {
        writeError?.let { throw it }
        stats.value = stats.value.map { if (it.id == stat.id) stat else it }
    }

    override suspend fun deleteStat(id: String) {
        writeError?.let { throw it }
        stats.value = stats.value.filterNot { it.id == id }
        entries.value = entries.value.filterKeys { it.first != id }
    }

    override suspend fun saveEntry(entry: StatEntry) {
        writeError?.let { throw it }
        entries.value += (entry.statId to entry.date) to entry
    }

    override suspend fun deleteEntry(statId: String, date: LocalDate) {
        writeError?.let { throw it }
        entries.value -= statId to date
    }

    fun put(vararg logged: StatEntry) {
        entries.value += logged.associateBy { it.statId to it.date }
    }
}

class FakeExerciseCatalogRepository : ExerciseCatalogRepository {
    /** Returned for every query. */
    var results: List<CatalogExercise> = emptyList()

    /** When set, every search fails with it. */
    var error: Exception? = null
    val queries = mutableListOf<String>()

    override suspend fun search(query: String): List<CatalogExercise> {
        queries += query
        error?.let { throw it }
        return results
    }
}

class FakeRestAlarm : RestAlarm {
    /** The pending alert: workout, kind and end time; null when none. */
    var scheduled: Triple<String, RestKind, Instant>? = null
    var cancelCalls = 0

    override fun schedule(workoutId: String, kind: RestKind, endsAt: Instant) {
        scheduled = Triple(workoutId, kind, endsAt)
    }

    override fun cancel() {
        scheduled = null
        cancelCalls++
    }
}
