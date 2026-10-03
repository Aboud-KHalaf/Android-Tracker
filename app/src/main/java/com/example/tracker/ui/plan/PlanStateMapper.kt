package com.example.tracker.ui.plan

import com.example.tracker.domain.model.Exercise
import com.example.tracker.domain.model.PlanDetails
import com.example.tracker.domain.model.Workout
import com.example.tracker.domain.model.WorkoutPlan
import java.time.ZoneId

/** Maps domain data to [PlanUiState]. Pure, so it can be tested without a ViewModel. */
internal class PlanStateMapper(private val zone: ZoneId) {

    fun map(
        details: PlanDetails?,
        summary: WorkoutPlan?,
        activeWorkout: Workout?,
        library: List<Exercise>,
    ): PlanUiState {
        if (details == null) return PlanUiState.NotFound
        val lastIndex = details.exercises.lastIndex
        val inPlan = details.exercises.map { it.exercise.id }.toSet()
        return PlanUiState.Success(
            name = details.name,
            lastDoneOn = summary?.lastDoneAt?.atZone(zone)?.toLocalDate(),
            exercises = details.exercises.mapIndexed { index, planExercise ->
                PlanExerciseUi(
                    id = planExercise.id,
                    name = planExercise.exercise.name,
                    type = planExercise.exercise.type,
                    targetSets = planExercise.targetSets,
                    canMoveUp = index > 0,
                    canMoveDown = index < lastIndex,
                )
            },
            availableExercises = library
                .filter { it.id !in inPlan }
                .map { ExerciseOptionUi(it.id, it.name, it.type) },
            workoutAction = when {
                activeWorkout != null -> WorkoutActionUi.Resume(activeWorkout.id)
                details.exercises.isEmpty() -> WorkoutActionUi.Unavailable
                else -> WorkoutActionUi.Start
            },
        )
    }
}
