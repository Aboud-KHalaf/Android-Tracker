package com.example.tracker.domain.workout

import com.example.tracker.domain.model.Workout
import com.example.tracker.domain.repository.WorkoutRepository
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

/**
 * The exercise to reopen a workout on: the one the user last had open, else the first with
 * sets left to do, else the last one.
 */
fun Workout.resumeExerciseIndex(): Int {
    val ordered = exercises.sortedBy { it.position }
    currentExerciseIndex?.takeIf { it in ordered.indices }?.let { return it }
    val open = ordered.indexOfFirst { exercise -> exercise.sets.any { !it.isCompleted } }
    return if (open >= 0) open else ordered.lastIndex.coerceAtLeast(0)
}

/** What the "workout in progress" notification shows. */
data class ActiveWorkoutStatus(
    val workoutId: String,
    val name: String,
    /** The clock runs from here, so it stays right however long the app is away. */
    val startedAt: Instant,
    val currentExerciseName: String?,
    val completedSets: Int,
    val totalSets: Int,
)

fun Workout.toStatus(): ActiveWorkoutStatus {
    val sets = exercises.flatMap { it.sets }
    return ActiveWorkoutStatus(
        workoutId = id,
        name = name,
        startedAt = startedAt,
        currentExerciseName = exercises.sortedBy { it.position }.getOrNull(resumeExerciseIndex())?.exercise?.name,
        completedSets = sets.count { it.isCompleted },
        totalSets = sets.size,
    )
}

/** Shows or removes the "workout in progress" notification. */
interface ActiveWorkoutNotifier {
    fun show(status: ActiveWorkoutStatus)
    fun cancel()
}

/**
 * Keeps the "workout in progress" notification in line with the stored workout.
 *
 * Everything about a session lives in the database, and the clock is worked out from the
 * stored start time, so nothing has to keep running in the background: the notification only
 * mirrors the database. It is refreshed whenever the workout changes while the app runs, and
 * by [syncNow] after events such as a reboot, which clear notifications.
 */
class ActiveWorkoutSession(
    private val workouts: WorkoutRepository,
    private val notifier: ActiveWorkoutNotifier,
) {
    private val statuses: Flow<ActiveWorkoutStatus?> =
        workouts.observeActiveWorkout().map { it?.toStatus() }.distinctUntilChanged()

    /** Updates the notification on every change for as long as [scope] lives. */
    fun keepInSync(scope: CoroutineScope): Job = statuses.onEach(::render).launchIn(scope)

    /** Shows the notification once if a workout is in progress, or removes a stale one. */
    suspend fun syncNow() = render(statuses.first())

    private fun render(status: ActiveWorkoutStatus?) {
        if (status == null) notifier.cancel() else notifier.show(status)
    }
}
