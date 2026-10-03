package com.example.tracker.ui.exercises

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.tracker.domain.model.ExerciseType
import com.example.tracker.ui.theme.TrackerTheme

@Preview(name = "Light", showBackground = true, heightDp = 600)
@Preview(name = "Dark", showBackground = true, heightDp = 600, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ExerciseListPreview() {
    TrackerTheme {
        ExerciseListScreen(
            uiState = ExerciseListUiState.Success(
                listOf(
                    ExerciseItemUi("bench", "Bench Press", ExerciseType.WEIGHT_REPS),
                    ExerciseItemUi("incline", "Incline Dumbbell Press", ExerciseType.WEIGHT_REPS),
                    ExerciseItemUi("plank", "Plank", ExerciseType.DURATION),
                ),
            ),
            onOpenExercise = {},
            onRetry = {},
        )
    }
}

@Preview(name = "Empty", showBackground = true, heightDp = 500)
@Composable
private fun ExerciseListEmptyPreview() {
    TrackerTheme { ExerciseListScreen(ExerciseListUiState.Success(emptyList()), onOpenExercise = {}, onRetry = {}) }
}
