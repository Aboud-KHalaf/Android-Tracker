package com.example.tracker.ui.catalog

import com.example.tracker.domain.model.ExerciseType

/** Everything the Exercise catalog screen renders. */
data class ExerciseCatalogUiState(
    val query: String = "",
    val results: CatalogResultsUi = CatalogResultsUi.Loading,
    /** The exercise whose details are open, if any. */
    val selected: CatalogExerciseUi? = null,
    val isAdding: Boolean = false,
)

sealed interface CatalogResultsUi {
    data object Loading : CatalogResultsUi

    /** The catalog couldn't be reached, e.g. while offline. */
    data object Error : CatalogResultsUi

    /** Matching exercises; empty when nothing matches the query. */
    data class Success(val exercises: List<CatalogExerciseUi>) : CatalogResultsUi
}

data class CatalogExerciseUi(
    val id: Int,
    val name: String,
    val category: String?,
    val primaryMuscles: List<String>,
    val secondaryMuscles: List<String>,
    val equipment: List<String>,
    val description: String,
    val imageUrl: String?,
    /** Preselected when adding it; the user can change it. */
    val suggestedType: ExerciseType,
    /** The plan already has an exercise with this name. */
    val isInPlan: Boolean,
)
