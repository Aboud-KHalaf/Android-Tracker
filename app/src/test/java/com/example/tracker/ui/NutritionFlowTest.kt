package com.example.tracker.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.test.core.app.ApplicationProvider
import com.example.tracker.MainActivity
import com.example.tracker.TrackerApplication
import com.example.tracker.domain.model.DailyNutrition
import com.example.tracker.domain.model.NutritionTargets
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Logging today's calories and protein, setting targets, and deleting the day with undo. */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
// Default screen: with the xxhdpi qualifier the other flow tests use, Robolectric never idles
// once a dialog with a text field is open.
@Config(sdk = [35])
class NutritionFlowTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val container get() = ApplicationProvider.getApplicationContext<TrackerApplication>().container
    private val today get() = container.time.now().atZone(container.time.zone()).toLocalDate()

    @Test
    fun logTodaySetTargetsAndDeleteWithUndo() {
        compose.onNodeWithText("Nutrition").performClick()
        waitForNode(hasText("Nothing logged today yet", substring = true))

        // Log today.
        compose.onNodeWithText("Log today").performClick()
        waitForNode(hasSetTextAction() and hasAnyAncestor(isDialog()))
        inDialog(hasSetTextAction() and hasText("Calories")).performTextInput("2300")
        inDialog(hasSetTextAction() and hasText("Protein")).performTextInput("150")
        inDialog(hasText("Save")).performClick()

        waitForNode(hasText("2,300 kcal"))
        assertEquals(DailyNutrition(today, 2300, 150), stored(today))

        // Targets turn the amounts into progress against them.
        compose.onNodeWithText("Targets").performClick()
        inDialog(hasSetTextAction() and hasText("Calories")).performTextInput("2400")
        inDialog(hasSetTextAction() and hasText("Protein")).performTextInput("160")
        inDialog(hasText("Save")).performClick()

        waitForNode(hasText("2,300 / 2,400 kcal"))
        compose.onNodeWithText("150 / 160 g").assertExists()
        assertEquals(NutritionTargets(2400, 160), container.settingsRepository.nutritionTargets.value)

        // Delete from the history row, then undo.
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("2,300 kcal · 150 g protein"))
        compose.onNodeWithText("2,300 kcal · 150 g protein").performClick()
        waitForNode(hasText("Edit day"))
        inDialog(hasText("Delete")).performClick()

        waitForNode(hasText("Day deleted"))
        assertNull(stored(today))
        compose.onNodeWithText("Undo").performClick()

        waitForNode(hasText("2,300 / 2,400 kcal"))
        assertEquals(DailyNutrition(today, 2300, 150), stored(today))
    }

    private fun inDialog(matcher: SemanticsMatcher) = compose.onNode(matcher and hasAnyAncestor(isDialog()))

    private fun stored(date: LocalDate): DailyNutrition? = runBlocking {
        container.nutritionRepository.observeDay(date).first()
    }

    /** Waits for a node matching [matcher]; Room loads data on background threads. */
    private fun waitForNode(matcher: SemanticsMatcher, timeoutMillis: Long = 5_000) {
        compose.waitUntilAtLeastOneExists(matcher, timeoutMillis)
    }
}
