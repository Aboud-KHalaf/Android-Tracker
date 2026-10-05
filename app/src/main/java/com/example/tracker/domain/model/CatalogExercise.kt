package com.example.tracker.domain.model

/**
 * An exercise from the online exercise catalog, before it's added to the user's library.
 * [description] is plain text; [imageUrl] is null when the catalog has no picture of it.
 */
data class CatalogExercise(
    val id: Int,
    val name: String,
    val category: String?,
    val primaryMuscles: List<String>,
    val secondaryMuscles: List<String>,
    val equipment: List<String>,
    val description: String,
    val imageUrl: String?,
)
