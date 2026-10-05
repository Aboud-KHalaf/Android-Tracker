package com.example.tracker.ui.catalog

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.tracker.domain.model.ExerciseType
import com.example.tracker.ui.theme.TrackerTheme

private fun previewExercise(id: Int, name: String, category: String, equipment: List<String>, isInPlan: Boolean = false) =
    CatalogExerciseUi(
        id = id,
        name = name,
        category = category,
        primaryMuscles = listOf("Chest"),
        secondaryMuscles = emptyList(),
        equipment = equipment,
        description = "",
        imageUrl = null,
        suggestedType = ExerciseType.WEIGHT_REPS,
        isInPlan = isInPlan,
    )

@Preview(name = "Light", showBackground = true, heightDp = 600)
@Preview(name = "Dark", showBackground = true, heightDp = 600, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ExerciseCatalogPreview() {
    TrackerTheme {
        ExerciseCatalogScreen(
            uiState = ExerciseCatalogUiState(
                query = "bench",
                results = CatalogResultsUi.Success(
                    listOf(
                        previewExercise(73, "Bench Press", "Chest", listOf("Barbell", "Bench"), isInPlan = true),
                        previewExercise(75, "Benchpress Dumbbells", "Chest", listOf("Bench", "Dumbbell")),
                        previewExercise(76, "Bench Press Narrow Grip", "Arms", listOf("Barbell", "Bench")),
                    ),
                ),
            ),
            actions = ExerciseCatalogActions(),
        )
    }
}

@Preview(name = "Error", showBackground = true, heightDp = 500)
@Composable
private fun ExerciseCatalogErrorPreview() {
    TrackerTheme {
        ExerciseCatalogScreen(ExerciseCatalogUiState(results = CatalogResultsUi.Error), ExerciseCatalogActions())
    }
}
