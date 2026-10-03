package com.example.tracker.ui.plan

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.tracker.domain.model.ExerciseType
import com.example.tracker.ui.theme.TrackerTheme
import java.time.LocalDate

private val previewExercises = listOf(
    "Bench Press" to ExerciseType.WEIGHT_REPS,
    "Incline Dumbbell Press" to ExerciseType.WEIGHT_REPS,
    "Overhead Press" to ExerciseType.WEIGHT_REPS,
    "Triceps Pushdown" to ExerciseType.WEIGHT_REPS,
    "Plank" to ExerciseType.DURATION,
).mapIndexed { index, (name, type) ->
    PlanExerciseUi("pe$index", name, type, 3, canMoveUp = index > 0, canMoveDown = index < 4)
}

private val previewState = PlanUiState.Success(
    name = "Push Day",
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
