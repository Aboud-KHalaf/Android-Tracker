package com.example.tracker.data.remote.wger

import com.example.tracker.data.remote.HttpClient
import java.net.URLEncoder
import kotlinx.serialization.json.Json

/**
 * The public, key-free part of the wger REST API (https://wger.de/api/v2/).
 * Exercise data is licensed CC-BY-SA by the wger project and its contributors.
 */
internal class WgerApi(
    private val http: HttpClient,
    private val baseUrl: String = BASE_URL,
) {
    private val json = Json { ignoreUnknownKeys = true }

    /** English exercises whose name contains [query]; a blank query isn't filtered. */
    suspend fun searchExercises(query: String, limit: Int): List<WgerExerciseInfoDto> {
        val url = buildString {
            append(baseUrl).append("exerciseinfo/?language=").append(ENGLISH).append("&limit=").append(limit)
            if (query.isNotBlank()) append("&name__search=").append(URLEncoder.encode(query.trim(), "UTF-8"))
        }
        return json.decodeFromString<WgerPageDto<WgerExerciseInfoDto>>(http.get(url)).results
    }

    companion object {
        const val BASE_URL = "https://wger.de/api/v2/"

        /** wger's id for English. */
        const val ENGLISH = 2
    }
}
