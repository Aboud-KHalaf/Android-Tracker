package com.example.tracker.ui.catalog

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.example.tracker.domain.model.ExerciseType
import com.example.tracker.ui.theme.TrackerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class ExerciseCatalogScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val plank = CatalogExerciseUi(
        id = 238,
        name = "Plank",
        category = "Abs",
        primaryMuscles = listOf("Abs"),
        secondaryMuscles = emptyList(),
        equipment = listOf("none (bodyweight exercise)"),
        description = "Hold your body straight.",
        imageUrl = null,
        suggestedType = ExerciseType.DURATION,
        isInPlan = false,
    )

    @Test
    fun typingSearchesAndPickingAResultForwardsItsId() {
        val queries = mutableListOf<String>()
        var selected: Int? = null
        compose.setContent {
            TrackerTheme {
                ExerciseCatalogScreen(
                    uiState = ExerciseCatalogUiState(results = CatalogResultsUi.Success(listOf(plank))),
                    actions = ExerciseCatalogActions(onQueryChange = { queries += it }, onSelect = { selected = it }),
                )
            }
        }

        compose.onNodeWithText("Search exercises").performTextInput("pla")
        compose.onNodeWithText("Plank").performClick()

        assertEquals(listOf("pla"), queries)
        assertEquals(238, selected)
        compose.onNodeWithText("Exercise data from wger.de, licensed CC-BY-SA.").assertExists()
    }

    @Test
    fun detailsShowTheExerciseAndAddWithTheSuggestedType() {
        var added: ExerciseType? = null
        compose.setContent {
            TrackerTheme {
                ExerciseCatalogScreen(
                    uiState = ExerciseCatalogUiState(results = CatalogResultsUi.Success(listOf(plank)), selected = plank),
                    actions = ExerciseCatalogActions(onAdd = { added = it }),
                )
            }
        }

        compose.onNodeWithText("Hold your body straight.").assertExists()
        compose.onNodeWithText("Add to plan").performScrollTo().performClick()

        assertEquals(ExerciseType.DURATION, added)
    }

    @Test
    fun exerciseAlreadyInPlan_isMarkedAndCantBeAddedAgain() {
        val inPlan = plank.copy(isInPlan = true)
        compose.setContent {
            TrackerTheme {
                ExerciseCatalogScreen(
                    uiState = ExerciseCatalogUiState(results = CatalogResultsUi.Success(listOf(inPlan)), selected = inPlan),
                    actions = ExerciseCatalogActions(),
                )
            }
        }

        compose.onNodeWithContentDescription("In plan").assertExists()
        compose.onNodeWithText("Already in this plan").assertExists()
        compose.onNodeWithText("Add to plan").assertDoesNotExist()
    }

    @Test
    fun offline_showsErrorWithRetry() {
        var retried = false
        compose.setContent {
            TrackerTheme {
                ExerciseCatalogScreen(
                    uiState = ExerciseCatalogUiState(results = CatalogResultsUi.Error),
                    actions = ExerciseCatalogActions(onRetry = { retried = true }),
                )
            }
        }

        compose.onNodeWithText("Retry").performClick()

        assertEquals(true, retried)
    }
}
