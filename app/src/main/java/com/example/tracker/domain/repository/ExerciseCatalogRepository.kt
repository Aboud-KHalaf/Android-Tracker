package com.example.tracker.domain.repository

import com.example.tracker.domain.model.CatalogExercise

/**
 * Read-only online exercise catalog. Network and parsing failures propagate as exceptions,
 * so callers can show an error and offer a retry.
 */
interface ExerciseCatalogRepository {
    /** Exercises whose name matches [query]; a blank query returns the start of the catalog. */
    suspend fun search(query: String): List<CatalogExercise>
}
