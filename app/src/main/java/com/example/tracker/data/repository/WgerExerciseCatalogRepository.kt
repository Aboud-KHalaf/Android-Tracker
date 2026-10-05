package com.example.tracker.data.repository

import com.example.tracker.data.remote.wger.WgerApi
import com.example.tracker.data.remote.wger.toDomain
import com.example.tracker.domain.model.CatalogExercise
import com.example.tracker.domain.repository.ExerciseCatalogRepository

/** Searches the wger exercise catalog online; nothing is stored locally. */
internal class WgerExerciseCatalogRepository(
    private val api: WgerApi,
) : ExerciseCatalogRepository {

    override suspend fun search(query: String): List<CatalogExercise> =
        api.searchExercises(query, PAGE_SIZE)
            .mapNotNull { it.toDomain() }
            .distinctBy { it.name.lowercase() }

    private companion object {
        const val PAGE_SIZE = 40
    }
}
