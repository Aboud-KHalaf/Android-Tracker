package com.example.tracker.ui.workout

import com.example.tracker.domain.model.ExerciseType
import com.example.tracker.domain.model.Workout
import com.example.tracker.domain.model.WorkoutExercise
import com.example.tracker.domain.model.WorkoutSet
import com.example.tracker.domain.progress.improvementOver
import com.example.tracker.ui.common.SetValueUi
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
    ): ActiveWorkoutUiState {
        if (workout == null || !workout.isInProgress) return ActiveWorkoutUiState.NotFound
        val exercises = workout.exercises.sortedBy { it.position }
        if (exercises.isEmpty()) return ActiveWorkoutUiState.NotFound

        val index = (selection.exerciseIndex ?: defaultExerciseIndex(workout)).coerceIn(exercises.indices)
        val current = exercises[index]
        val sets = current.sets.sortedBy { it.position }
        val lastTime = lastTimes[current.exercise.id].orEmpty().sortedBy { it.position }
        // A selected set stays active even while its reopening is being saved.
        val activeId = selection.setId?.takeIf { id -> sets.any { it.id == id } }
            ?: sets.firstOrNull { !it.isCompleted }?.id
        val allSets = exercises.flatMap { it.sets }

        return ActiveWorkoutUiState.Success(
            workoutName = workout.name,
            startedAt = workout.startedAt,
            exerciseIndex = index,
            exerciseCount = exercises.size,
            exercise = CurrentExerciseUi(
                workoutExerciseId = current.id,
                exerciseId = current.exercise.id,
                name = current.exercise.name,
                type = current.exercise.type,
                lastTime = lastTime.mapNotNull { it.toValue() },
            ),
            sets = sets.mapIndexed { i, set ->
                val previous = lastTime.getOrNull(i)
                when {
                    set.id == activeId -> SetRowUi.Active(
                        id = set.id,
                        number = i + 1,
                        lastTime = previous?.toValue(),
                        editor = editorFor(current, set, previous, draft),
                    )

                    set.isCompleted -> SetRowUi.Done(
                        id = set.id,
                        number = i + 1,
                        value = set.toValue(),
                        improvement = improvementOver(current.exercise.type, set, previous),
                    )

                    else -> SetRowUi.Upcoming(id = set.id, number = i + 1, planned = set.toValue())
                }
            },
            previousExerciseName = exercises.getOrNull(index - 1)?.exercise?.name,
            nextExerciseName = exercises.getOrNull(index + 1)?.exercise?.name,
            completedSetCount = allSets.count { it.isCompleted },
            openSetCount = allSets.count { !it.isCompleted },
        )
    }

    /** The first exercise with sets left to do, else the last one. */
    fun defaultExerciseIndex(workout: Workout): Int {
        val exercises = workout.exercises.sortedBy { it.position }
        val open = exercises.indexOfFirst { exercise -> exercise.sets.any { !it.isCompleted } }
        return if (open >= 0) open else exercises.lastIndex.coerceAtLeast(0)
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

            // An open set's stored duration is last time's prefill, i.e. the target, so the
            // timer starts from zero unless a draft (e.g. a reopened set) says otherwise.
            ExerciseType.DURATION -> SetEditorUi.Duration(
                bankedSeconds = (ownDraft as? SetDraft.Hold)?.bankedSeconds ?: 0,
                runningSince = (ownDraft as? SetDraft.Hold)?.runningSince,
                targetSeconds = previous?.durationSeconds,
            )
        }
    }

    private fun WorkoutSet.toValue(): SetValueUi? = when {
        weightKg != null && reps != null -> SetValueUi.WeightReps(weightKg, reps)
        durationSeconds != null -> SetValueUi.Hold(durationSeconds)
        else -> null
    }
}
