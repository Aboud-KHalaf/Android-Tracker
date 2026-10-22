package com.example.tracker.ui.workout

import com.example.tracker.domain.model.ExerciseType
import com.example.tracker.domain.model.SetImprovement
import com.example.tracker.domain.model.SetType
import com.example.tracker.domain.workout.RestKind
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
        /** The rest after the last completed set, while it runs. */
        val rest: RestUi? = null,
    ) : ActiveWorkoutUiState {
        /** Every set of the current exercise is completed. */
        val allSetsDone: Boolean get() = sets.none { it is SetRowUi.Active || it is SetRowUi.Upcoming }

        /** Something is being timed (a hold or a rest), so the display should stay on. */
        val isTiming: Boolean
            get() = rest != null || sets.any { row ->
                val editor = (row as? SetRowUi.Active)?.editor
                editor is SetEditorUi.Duration && editor.isRunning
            }
    }
}

/**
 * A rest countdown that ends at [endsAt]; the UI ticks it, so this state only changes on user
 * actions. [totalSeconds] is the whole rest, for showing how much is left.
 */
data class RestUi(val kind: RestKind, val endsAt: Instant, val totalSeconds: Int) {
    /** Whole seconds left, rounded up so the countdown shows 0:00 only once it is over. */
    fun remainingSecondsAt(now: Instant): Int {
        val millis = java.time.Duration.between(now, endsAt).toMillis()
        return if (millis <= 0) 0 else ((millis + MILLIS_PER_SECOND - 1) / MILLIS_PER_SECOND).toInt()
    }

    /** The share of the rest still left, 1 at the start and 0 at the end. */
    fun fractionLeftAt(now: Instant): Float =
        if (totalSeconds <= 0) 0f else (remainingSecondsAt(now).toFloat() / totalSeconds).coerceIn(0f, 1f)

    private companion object {
        const val MILLIS_PER_SECOND = 1_000L
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

/**
 * One set of the current exercise. [number] is one-based and counts warm-ups and working sets
 * separately: "Warm-up 1, Warm-up 2, Set 1, Set 2".
 */
sealed interface SetRowUi {
    val id: String
    val number: Int
    val type: SetType

    data class Done(
        override val id: String,
        override val number: Int,
        val value: SetValueUi?,
        /** How it beat the same set last time, if it did. */
        val improvement: SetImprovement?,
        override val type: SetType = SetType.NORMAL,
    ) : SetRowUi

    /** The set being edited. */
    data class Active(
        override val id: String,
        override val number: Int,
        val lastTime: SetValueUi?,
        val editor: SetEditorUi,
        override val type: SetType = SetType.NORMAL,
    ) : SetRowUi

    data class Upcoming(
        override val id: String,
        override val number: Int,
        val planned: SetValueUi?,
        override val type: SetType = SetType.NORMAL,
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

    /**
     * A timed hold. The hold is [bankedSeconds] plus, while running, the time since
     * [runningSince]; the UI ticks it, so this state only changes on user actions.
     */
    data class Duration(
        val bankedSeconds: Int,
        val runningSince: Instant?,
        /** Last time's hold for this set, to beat. */
        val targetSeconds: Int?,
    ) : SetEditorUi {
        val isRunning: Boolean get() = runningSince != null

        fun secondsAt(now: Instant): Int {
            val running = runningSince?.let { java.time.Duration.between(it, now).seconds.toInt().coerceAtLeast(0) } ?: 0
            return bankedSeconds + running
        }

        fun statusAt(now: Instant): HoldStatus {
            val target = targetSeconds ?: return HoldStatus.NoTarget
            val seconds = secondsAt(now)
            return when {
                seconds > target -> HoldStatus.PastTarget(seconds - target)
                seconds == target -> HoldStatus.MatchedTarget
                else -> HoldStatus.BelowTarget(target)
            }
        }
    }
}

/** How a running hold compares with last time. */
sealed interface HoldStatus {
    data object NoTarget : HoldStatus
    data class BelowTarget(val targetSeconds: Int) : HoldStatus
    data object MatchedTarget : HoldStatus
    data class PastTarget(val seconds: Int) : HoldStatus
}
