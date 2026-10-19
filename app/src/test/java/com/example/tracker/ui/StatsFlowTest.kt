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
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.tracker.MainActivity
import com.example.tracker.TrackerApplication
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Creating a custom stat on the Stats tab and logging today's value. */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
// Default screen: with the xxhdpi qualifier the other flow tests use, Robolectric never idles
// once a dialog with a text field is open.
@Config(sdk = [35])
class StatsFlowTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val container get() = ApplicationProvider.getApplicationContext<TrackerApplication>().container

    @Test
    fun createStatAndLogToday_showsTheValue() {
        compose.onNodeWithText("Stats").performClick()
        waitForNode(hasText("Weight"))
        // The floating button's label is still animating in, so it's only in the unmerged tree.
        compose.onNodeWithText("New stat", useUnmergedTree = true).performClick()

        waitForNode(hasSetTextAction() and hasText("Name"))
        compose.onNode(hasSetTextAction() and hasText("Name")).performTextInput("Body fat")
        compose.onNode(hasSetTextAction() and hasText("Unit (optional)")).performTextInput("%")
        compose.onNodeWithText("Save").performClick()

        // The new stat opens, with nothing logged yet.
        waitForNode(hasText("Nothing logged yet", substring = true))
        compose.onNodeWithText("Log today").performClick()
        waitForNode(hasSetTextAction() and hasAnyAncestor(isDialog()))
        compose.onNode(hasSetTextAction() and hasAnyAncestor(isDialog())).performTextInput("19.5")
        compose.onNode(hasText("Save") and hasAnyAncestor(isDialog())).performClick()

        waitForNode(hasText("19.5 %"))
        val overview = runBlocking { container.customStatRepository.observeOverviews().first() }.single()
        assertEquals("Body fat", overview.stat.name)
        assertEquals(19.5, overview.latest!!.value, 0.0)
    }

    /** Waits for a node matching [matcher]; Room loads data on background threads. */
    private fun waitForNode(matcher: SemanticsMatcher, timeoutMillis: Long = 5_000) {
        compose.waitUntilAtLeastOneExists(matcher, timeoutMillis)
    }
}
