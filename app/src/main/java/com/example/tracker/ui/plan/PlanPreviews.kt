package com.example.tracker.ui.plan

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.tracker.domain.model.ExerciseConfig
import com.example.tracker.domain.model.ExerciseType
import com.example.tracker.ui.common.SetValueUi
import com.example.tracker.ui.theme.TrackerTheme
import java.time.DayOfWeek
import java.time.LocalDate

private val previewExercises = listOf(
    "Bench Press" to ExerciseType.WEIGHT_REPS,
    "Incline Dumbbell Press" to ExerciseType.WEIGHT_REPS,
    "Overhead Press" to ExerciseType.WEIGHT_REPS,
    "Triceps Pushdown" to ExerciseType.WEIGHT_REPS,
    "Plank" to ExerciseType.DURATION,
).mapIndexed { index, (name, type) ->
    val config = if (index == 0) ExerciseConfig(targetSets = 4, targetReps = 8, targetWeightKg = 60.0) else ExerciseConfig()
    PlanExerciseUi("pe$index", "e$index", name, type, config, canMoveUp = index > 0, canMoveDown = index < 4)
}

private val previewState = PlanUiState.Success(
    name = "Push",
    dayOfWeek = DayOfWeek.MONDAY,
    lastDoneOn = LocalDate.of(2026, 9, 28),
    exercises = previewExercises,
    availableExercises = emptyList(),
    workoutAction = WorkoutActionUi.Start,
)

@Preview(name = "Light", showBackground = true, heightDp = 844)
@Preview(name = "Dark", showBackground = true, heightDp = 844, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PlanScreenPreview() {
    TrackerTheme { PlanScreen(uiState = previewState, actions = PlanActions()) }
}

@Preview(name = "Empty", showBackground = true, heightDp = 844)
@Composable
private fun PlanScreenEmptyPreview() {
    TrackerTheme {
        PlanScreen(
            uiState = previewState.copy(exercises = emptyList(), lastDoneOn = null, workoutAction = WorkoutActionUi.Unavailable),
            actions = PlanActions(),
        )
    }
}

@Preview(name = "Edit targets", showBackground = true, heightDp = 844)
@Composable
private fun PlanScreenEditorPreview() {
    TrackerTheme {
        PlanScreen(
            uiState = previewState.copy(
                editor = ExerciseEditorUi(
                    planExerciseId = "pe0",
                    name = "Bench Press",
                    type = ExerciseType.WEIGHT_REPS,
                    config = ExerciseConfig(targetSets = 4, targetReps = 8, targetWeightKg = 60.0),
                    lastRecorded = listOf(SetValueUi.WeightReps(57.5, 8), SetValueUi.WeightReps(57.5, 7)),
                ),
            ),
            actions = PlanActions(),
        )
    }
}
