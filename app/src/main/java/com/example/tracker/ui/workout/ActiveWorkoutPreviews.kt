package com.example.tracker.ui.workout

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.tracker.domain.model.ExerciseType
import com.example.tracker.domain.model.SetImprovement
import com.example.tracker.ui.common.SetValueUi
import com.example.tracker.ui.theme.TrackerTheme
import java.time.Instant

private val previewState = ActiveWorkoutUiState.Success(
    workoutName = "Push Day",
    startedAt = Instant.now().minusSeconds(1_453),
    exerciseIndex = 0,
    exerciseCount = 5,
    exercise = CurrentExerciseUi(
        workoutExerciseId = "we1",
        exerciseId = "bench",
        name = "Bench Press",
        type = ExerciseType.WEIGHT_REPS,
        lastTime = listOf(SetValueUi.WeightReps(45.0, 10), SetValueUi.WeightReps(50.0, 8), SetValueUi.WeightReps(50.0, 8)),
    ),
    sets = listOf(
        SetRowUi.Done("s1", 1, SetValueUi.WeightReps(47.5, 10), SetImprovement.Weight(2.5)),
        SetRowUi.Active("s2", 2, SetValueUi.WeightReps(50.0, 8), SetEditorUi.WeightReps("50", "8")),
        SetRowUi.Upcoming("s3", 3, SetValueUi.WeightReps(50.0, 8)),
    ),
    previousExerciseName = null,
    nextExerciseName = "Incline Dumbbell Press",
    completedSetCount = 1,
    openSetCount = 14,
)

@Preview(name = "Light", showBackground = true, heightDp = 844)
@Preview(name = "Dark", showBackground = true, heightDp = 844, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ActiveWorkoutScreenPreview() {
    TrackerTheme { ActiveWorkoutScreen(uiState = previewState, actions = ActiveWorkoutActions()) }
}

@Preview(name = "All sets done, last exercise", showBackground = true, heightDp = 844)
@Composable
private fun ActiveWorkoutAllDonePreview() {
    TrackerTheme {
        ActiveWorkoutScreen(
            uiState = previewState.copy(
                exerciseIndex = 4,
                sets = listOf(
                    SetRowUi.Done("s1", 1, SetValueUi.WeightReps(47.5, 10), SetImprovement.Weight(2.5)),
                    SetRowUi.Done("s2", 2, SetValueUi.WeightReps(50.0, 9), SetImprovement.Reps(1)),
                ),
                previousExerciseName = "Triceps Pushdown",
                nextExerciseName = null,
            ),
            actions = ActiveWorkoutActions(),
        )
    }
}

@Preview(name = "Timed hold", showBackground = true, heightDp = 844)
@Preview(name = "Timed hold dark", showBackground = true, heightDp = 844, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ActiveWorkoutHoldPreview() {
    TrackerTheme {
        ActiveWorkoutScreen(
            uiState = previewState.copy(
                exerciseIndex = 4,
                exercise = CurrentExerciseUi(
                    workoutExerciseId = "we5",
                    exerciseId = "plank",
                    name = "Plank",
                    type = ExerciseType.DURATION,
                    lastTime = listOf(SetValueUi.Hold(45), SetValueUi.Hold(40), SetValueUi.Hold(35)),
                ),
                sets = listOf(
                    SetRowUi.Done("h1", 1, SetValueUi.Hold(50), SetImprovement.Hold(5)),
                    SetRowUi.Active("h2", 2, SetValueUi.Hold(40), SetEditorUi.Duration(28, null, 40)),
                    SetRowUi.Upcoming("h3", 3, SetValueUi.Hold(35)),
                ),
                previousExerciseName = "Triceps Pushdown",
                nextExerciseName = null,
            ),
            actions = ActiveWorkoutActions(),
        )
    }
}
