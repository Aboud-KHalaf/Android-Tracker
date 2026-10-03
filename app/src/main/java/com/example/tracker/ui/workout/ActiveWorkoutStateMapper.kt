package com.example.tracker.ui.workout

import com.example.tracker.domain.model.ExerciseType
import com.example.tracker.domain.model.Workout
import com.example.tracker.domain.model.WorkoutExercise
import com.example.tracker.domain.model.WorkoutSet
import com.example.tracker.domain.progress.improvementOver
import com.example.tracker.ui.common.SetValueUi

/** What the user has picked; null means "the default". */
internal data class WorkoutSelection(val exerciseIndex: Int? = null, val setId: String? = null)

/** Text typed into the active set that isn't saved yet. */
internal data class SetDraft(val setId: String, val weightText: String, val repsText: String)

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
                        editor = editorFor(current, set, draft),
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

    private fun editorFor(exercise: WorkoutExercise, set: WorkoutSet, draft: SetDraft?): SetEditorUi =
        when (exercise.exercise.type) {
            ExerciseType.WEIGHT_REPS -> if (draft != null && draft.setId == set.id) {
                SetEditorUi.WeightReps(draft.weightText, draft.repsText)
            } else {
                SetEditorUi.WeightReps(SetInput.formatWeight(set.weightKg), SetInput.formatReps(set.reps))
            }

            ExerciseType.DURATION -> SetEditorUi.Duration
        }

    private fun WorkoutSet.toValue(): SetValueUi? = when {
        weightKg != null && reps != null -> SetValueUi.WeightReps(weightKg, reps)
        durationSeconds != null -> SetValueUi.Hold(durationSeconds)
        else -> null
    }
}
