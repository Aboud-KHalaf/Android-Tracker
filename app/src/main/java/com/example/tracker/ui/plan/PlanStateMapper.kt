package com.example.tracker.ui.plan

import com.example.tracker.domain.model.Exercise
import com.example.tracker.domain.model.PlanDetails
import com.example.tracker.domain.model.Workout
import com.example.tracker.domain.model.WorkoutPlan
import com.example.tracker.domain.model.WorkoutSet
import com.example.tracker.domain.workout.Supersets
import com.example.tracker.ui.common.toSetValue
import java.time.ZoneId

/** The exercise whose targets are being edited, with what was recorded for it last time. */
internal data class EditTarget(val planExerciseId: String, val lastRecorded: List<WorkoutSet>, val isSaving: Boolean = false)

/** Maps domain data to [PlanUiState]. Pure, so it can be tested without a ViewModel. */
internal class PlanStateMapper(private val zone: ZoneId) {
    private companion object {
        /** Supersets are lettered A to Z, then start again. */
        const val LETTERS = 26
    }


    fun map(
        details: PlanDetails?,
        summary: WorkoutPlan?,
        activeWorkout: Workout?,
        library: List<Exercise>,
        editing: EditTarget?,
    ): PlanUiState {
        if (details == null) return PlanUiState.NotFound
        val lastIndex = details.exercises.lastIndex
        val inPlan = details.exercises.map { it.exercise.id }.toSet()
        val supersetLabels = Supersets.runs(details.exercises.map { it.supersetGroup })
            .flatMapIndexed { number, run -> run.map { it to ('A' + number % LETTERS).toString() } }
            .toMap()
        val edited = editing?.let { target -> details.exercises.firstOrNull { it.id == target.planExerciseId } }
        return PlanUiState.Success(
            name = details.name,
            dayOfWeek = details.dayOfWeek,
            lastDoneOn = summary?.lastDoneAt?.atZone(zone)?.toLocalDate(),
            exercises = details.exercises.mapIndexed { index, planExercise ->
                PlanExerciseUi(
                    id = planExercise.id,
                    exerciseId = planExercise.exercise.id,
                    name = planExercise.exercise.name,
                    type = planExercise.exercise.type,
                    config = planExercise.config,
                    canMoveUp = index > 0,
                    canMoveDown = index < lastIndex,
                    supersetLabel = supersetLabels[index],
                )
            },
            availableExercises = library
                .filter { it.id !in inPlan }
                .map { ExerciseOptionUi(it.id, it.name, it.type, it.muscleGroup) },
            workoutAction = when {
                activeWorkout != null -> WorkoutActionUi.Resume(activeWorkout.id)
                details.exercises.isEmpty() -> WorkoutActionUi.Unavailable
                else -> WorkoutActionUi.Start
            },
            // An exercise removed while its sheet is open just closes the sheet.
            editor = if (edited != null && editing != null) {
                ExerciseEditorUi(
                    planExerciseId = edited.id,
                    name = edited.exercise.name,
                    type = edited.exercise.type,
                    config = edited.config,
                    lastRecorded = editing.lastRecorded.mapNotNull { it.toSetValue() },
                    isSaving = editing.isSaving,
                )
            } else {
                null
            },
        )
    }
}
