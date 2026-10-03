package com.example.tracker.ui.workout

import com.example.tracker.domain.model.ExerciseType
import com.example.tracker.domain.model.SetImprovement
import com.example.tracker.ui.common.SetValueUi
import java.time.Instant

/** Everything the Active workout screen renders. */
sealed interface ActiveWorkoutUiState {
    data object Loading : ActiveWorkoutUiState
    data object Error : ActiveWorkoutUiState

    /** The workout doesn't exist or has already been finished. */
    data object NotFound : ActiveWorkoutUiState

    data class Success(
        val workoutName: String,
        val startedAt: Instant,
        /** Zero-based position of [exercise] in the workout. */
        val exerciseIndex: Int,
        val exerciseCount: Int,
        val exercise: CurrentExerciseUi,
        val sets: List<SetRowUi>,
        val previousExerciseName: String?,
        val nextExerciseName: String?,
        val completedSetCount: Int,
        val openSetCount: Int,
        val isFinishing: Boolean = false,
    ) : ActiveWorkoutUiState {
        /** Every set of the current exercise is completed. */
        val allSetsDone: Boolean get() = sets.none { it is SetRowUi.Active || it is SetRowUi.Upcoming }
    }
}

data class CurrentExerciseUi(
    val workoutExerciseId: String,
    val exerciseId: String,
    val name: String,
    val type: ExerciseType,
    /** The sets from the last finished workout with this exercise; empty the first time. */
    val lastTime: List<SetValueUi>,
)

/** One set of the current exercise. [number] is one-based. */
sealed interface SetRowUi {
    val id: String
    val number: Int

    data class Done(
        override val id: String,
        override val number: Int,
        val value: SetValueUi?,
        /** How it beat the same set last time, if it did. */
        val improvement: SetImprovement?,
    ) : SetRowUi

    /** The set being edited. */
    data class Active(
        override val id: String,
        override val number: Int,
        val lastTime: SetValueUi?,
        val editor: SetEditorUi,
    ) : SetRowUi

    data class Upcoming(
        override val id: String,
        override val number: Int,
        val planned: SetValueUi?,
    ) : SetRowUi
}

/** Input for the active set; its shape depends on the exercise type. */
sealed interface SetEditorUi {
    data class WeightReps(val weightText: String, val repsText: String) : SetEditorUi {
        val weightKg: Double? get() = SetInput.parseWeight(weightText)
        val reps: Int? get() = SetInput.parseReps(repsText)
        val isWeightValid: Boolean get() = weightKg != null
        val isRepsValid: Boolean get() = reps != null
        val canComplete: Boolean get() = isWeightValid && isRepsValid
    }

    /** Timed sets get their own editor; until then they can't be edited here. */
    data object Duration : SetEditorUi
}
