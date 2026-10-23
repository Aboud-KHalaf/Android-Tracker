package com.example.tracker.ui.workout

import com.example.tracker.domain.model.ExerciseType
import com.example.tracker.domain.model.Workout
import com.example.tracker.domain.model.WorkoutExercise
import com.example.tracker.domain.model.WorkoutSet
import com.example.tracker.domain.progress.bestValue
import com.example.tracker.domain.progress.improvementOver
import com.example.tracker.domain.workout.Supersets
import com.example.tracker.domain.progress.recordSetIds
import com.example.tracker.domain.workout.resumeExerciseIndex
import com.example.tracker.ui.common.SetValueUi
import com.example.tracker.ui.common.toSetValue
import java.time.Instant

/** What the user has picked; null means "the default". */
internal data class WorkoutSelection(val exerciseIndex: Int? = null, val setId: String? = null)

/** Input for the active set that isn't saved yet. */
internal sealed interface SetDraft {
    val setId: String

    /** Text typed into the weight and reps fields. */
    data class WeightReps(override val setId: String, val weightText: String, val repsText: String) : SetDraft

    /** A hold being timed: [bankedSeconds] plus the time since [runningSince] while running. */
    data class Hold(override val setId: String, val bankedSeconds: Int, val runningSince: Instant?) : SetDraft
}

/** Maps domain data and the user's selection to [ActiveWorkoutUiState]. Pure. */
internal object ActiveWorkoutStateMapper {

    fun map(
        workout: Workout?,
        lastTimes: Map<String, List<WorkoutSet>>,
        selection: WorkoutSelection,
        draft: SetDraft?,
        lastNotes: Map<String, String?> = emptyMap(),
        /** Each exercise's all-time best before this workout, by exercise id; see [bestValue]. */
        bests: Map<String, Double?> = emptyMap(),
    ): ActiveWorkoutUiState {
        if (workout == null || !workout.isInProgress) return ActiveWorkoutUiState.NotFound
        val exercises = workout.exercises.sortedBy { it.position }
        if (exercises.isEmpty()) return ActiveWorkoutUiState.NotFound

        val index = (selection.exerciseIndex ?: workout.resumeExerciseIndex()).coerceIn(exercises.indices)
        val current = exercises[index]
        val sets = current.sets.sortedBy { it.position }
        val lastTime = lastTimes[current.exercise.id].orEmpty().sortedBy { it.position }
        // A selected set stays active even while its reopening is being saved.
        val activeId = selection.setId?.takeIf { id -> sets.any { it.id == id } }
            ?: sets.firstOrNull { !it.isCompleted }?.id
        val allSets = exercises.flatMap { it.sets }
        val records = recordSetIds(current.exercise.type, sets, bests[current.exercise.id])

        return ActiveWorkoutUiState.Success(
            workoutName = workout.name,
            startedAt = workout.startedAt,
            exerciseIndex = index,
            exerciseCount = exercises.size,
            exercise = currentExercise(exercises, index, lastTime, lastNotes),
            sets = numbered(sets).map { (set, number) ->
                // Last time only holds working sets, so a working set compares with the one in
                // its place among them; warm-ups have nothing to compare with.
                val previous = if (set.type.isWarmUp) null else lastTime.getOrNull(number - 1)
                when {
                    set.id == activeId -> SetRowUi.Active(
                        id = set.id,
                        number = number,
                        lastTime = previous?.toSetValue(),
                        editor = editorFor(current, set, previous, draft),
                        type = set.type,
                        rpe = set.rpe,
                    )

                    set.isCompleted -> SetRowUi.Done(
                        id = set.id,
                        number = number,
                        value = set.toSetValue(),
                        improvement = improvementOver(current.exercise.type, set, previous),
                        type = set.type,
                        rpe = set.rpe,
                        isRecord = set.id in records,
                    )

                    else -> SetRowUi.Upcoming(id = set.id, number = number, planned = set.toSetValue(), type = set.type)
                }
            },
            previousExerciseName = exercises.getOrNull(index - 1)?.exercise?.name,
            nextExerciseName = exercises.getOrNull(index + 1)?.exercise?.name,
            nextOpenExercise = nextOpenExercise(exercises, index),
            completedSetCount = allSets.count { it.isCompleted },
            openSetCount = allSets.count { !it.isCompleted },
            workoutNote = workout.note,
        )
    }

    private fun currentExercise(
        exercises: List<WorkoutExercise>,
        index: Int,
        lastTime: List<WorkoutSet>,
        lastNotes: Map<String, String?>,
    ): CurrentExerciseUi {
        val current = exercises[index]
        return CurrentExerciseUi(
            workoutExerciseId = current.id,
            exerciseId = current.exercise.id,
            name = current.exercise.name,
            type = current.exercise.type,
            lastTime = lastTime.mapNotNull { it.toSetValue() },
            note = current.note,
            lastTimeNote = lastNotes[current.exercise.id],
            supersetPartners = Supersets.runOf(exercises.map { it.supersetGroup }, index)
                .filter { it != index }
                .map { exercises[it].exercise.name },
        )
    }

    /** The first exercise after [current] with sets left, going round to the start; never [current] itself. */
    private fun nextOpenExercise(exercises: List<WorkoutExercise>, current: Int): NextExerciseUi? =
        (exercises.indices.drop(current + 1) + exercises.indices.take(current))
            .firstOrNull { i -> exercises[i].sets.any { !it.isCompleted } }
            ?.let { NextExerciseUi(it, exercises[it].exercise.name) }

    /** Each set with its one-based number, counting warm-ups and working sets separately. */
    private fun numbered(sets: List<WorkoutSet>): List<Pair<WorkoutSet, Int>> {
        var warmUps = 0
        var working = 0
        return sets.map { set -> set to if (set.type.isWarmUp) ++warmUps else ++working }
    }

    private fun editorFor(
        exercise: WorkoutExercise,
        set: WorkoutSet,
        previous: WorkoutSet?,
        draft: SetDraft?,
    ): SetEditorUi {
        val ownDraft = draft?.takeIf { it.setId == set.id }
        return when (exercise.exercise.type) {
            ExerciseType.WEIGHT_REPS -> if (ownDraft is SetDraft.WeightReps) {
                SetEditorUi.WeightReps(ownDraft.weightText, ownDraft.repsText)
            } else {
                SetEditorUi.WeightReps(SetInput.formatWeight(set.weightKg), SetInput.formatReps(set.reps))
            }

            // An open set's stored duration is its prefill (the configured target, else last
            // time), so the timer starts from zero unless a draft (e.g. a reopened set) says
            // otherwise. The hold to beat is last time's, or the target the first time.
            ExerciseType.DURATION -> SetEditorUi.Duration(
                bankedSeconds = (ownDraft as? SetDraft.Hold)?.bankedSeconds ?: 0,
                runningSince = (ownDraft as? SetDraft.Hold)?.runningSince,
                targetSeconds = previous?.durationSeconds ?: set.durationSeconds,
            )
        }
    }
}
