package com.example.tracker.data.repository

import com.example.tracker.data.remote.HttpClient
import com.example.tracker.data.remote.wger.WgerApi
import com.example.tracker.domain.model.CatalogExercise
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WgerExerciseCatalogRepositoryTest {

    private val requestedUrls = mutableListOf<String>()
    private var response = PAGE

    private val http = HttpClient { url ->
        requestedUrls += url
        response
    }
    private val repository = WgerExerciseCatalogRepository(WgerApi(http, baseUrl = "https://example.test/api/"))

    @Test
    fun search_requestsEnglishExercisesMatchingTheTrimmedQuery() = runTest {
        repository.search("  bench press ")

        assertEquals(
            listOf("https://example.test/api/exerciseinfo/?language=2&limit=40&name__search=bench+press"),
            requestedUrls,
        )
    }

    @Test
    fun search_blankQuery_isNotFiltered() = runTest {
        repository.search(" ")

        assertEquals(listOf("https://example.test/api/exerciseinfo/?language=2&limit=40"), requestedUrls)
    }

    @Test
    fun search_mapsEnglishNameMusclesEquipmentImageAndPlainDescription() = runTest {
        val bench = repository.search("bench").first()

        assertEquals(
            CatalogExercise(
                id = 73,
                name = "Bench Press",
                category = "Chest",
                primaryMuscles = listOf("Chest"),
                secondaryMuscles = listOf("Shoulders", "Triceps brachii"),
                equipment = listOf("Barbell", "Bench"),
                description = "Lie on the bench.\nPush the bar up & lower it.",
                imageUrl = "https://example.test/media/bench-main.png",
            ),
            bench,
        )
    }

    @Test
    fun search_skipsExercisesWithoutAnEnglishNameAndDuplicateNames() = runTest {
        val names = repository.search("bench").map { it.name }

        assertEquals(listOf("Bench Press", "Plank"), names)
    }

    @Test
    fun search_imageFallsBackToFirstWhenNoneIsMain() = runTest {
        val plank = repository.search("plank").single { it.name == "Plank" }

        assertEquals("https://example.test/media/plank.png", plank.imageUrl)
        assertTrue(plank.equipment.isEmpty())
    }

    @Test(expected = IOException::class)
    fun search_networkFailure_propagates() = runTest {
        WgerExerciseCatalogRepository(WgerApi({ throw IOException("offline") })).search("bench")
    }

    private companion object {
        val PAGE = """
            {
              "count": 4,
              "next": null,
              "results": [
                {
                  "id": 73,
                  "category": {"id": 11, "name": "Chest"},
                  "muscles": [{"id": 4, "name": "Pectoralis major", "name_en": "Chest"}],
                  "muscles_secondary": [
                    {"id": 2, "name": "Anterior deltoid", "name_en": "Shoulders"},
                    {"id": 5, "name": "Triceps brachii", "name_en": ""}
                  ],
                  "equipment": [{"id": 1, "name": "Barbell"}, {"id": 8, "name": "Bench"}],
                  "images": [
                    {"image": "https://example.test/media/bench-side.png", "is_main": false},
                    {"image": "https://example.test/media/bench-main.png", "is_main": true}
                  ],
                  "translations": [
                    {"name": "Bankdrücken", "description": "", "language": 1},
                    {"name": " Bench Press ", "description": "<p>Lie on the bench.</p>\n<p>Push the bar up &amp; lower it.</p>", "language": 2}
                  ]
                },
                {
                  "id": 74,
                  "category": {"id": 11, "name": "Chest"},
                  "translations": [{"name": "bench press", "description": "", "language": 2}]
                },
                {
                  "id": 99,
                  "category": {"id": 10, "name": "Abs"},
                  "translations": [{"name": "Nur Deutsch", "description": "", "language": 1}]
                },
                {
                  "id": 238,
                  "category": {"id": 10, "name": "Abs"},
                  "images": [{"image": "https://example.test/media/plank.png", "is_main": false}],
                  "translations": [{"name": "Plank", "language": 2}]
                }
              ]
            }
        """.trimIndent()
    }
}
