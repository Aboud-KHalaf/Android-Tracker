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
import com.example.tracker.domain.model.WeightEntry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Logging today's weight from the Weight tab. */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
// Default screen: with the xxhdpi qualifier the other flow tests use, Robolectric never idles
// once a dialog with a text field is open.
@Config(sdk = [35])
class WeightFlowTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val container get() = ApplicationProvider.getApplicationContext<TrackerApplication>().container

    @Test
    fun logTodaysWeight_showsItAsCurrent() {
        compose.onNodeWithText("Weight").performClick()
        waitForNode(hasText("No weight logged yet", substring = true))

        compose.onNodeWithText("Log today").performClick()
        waitForNode(hasSetTextAction() and hasAnyAncestor(isDialog()))
        compose.onNode(hasSetTextAction() and hasAnyAncestor(isDialog())).performTextInput("78.4")
        compose.onNode(hasText("Save") and hasAnyAncestor(isDialog())).performClick()

        waitForNode(hasText("Current weight"))
        waitForNode(hasText("78.4 kg"))
        val today = container.time.now().atZone(container.time.zone()).toLocalDate()
        assertEquals(WeightEntry(today, 78.4), runBlocking { container.weightRepository.observeEntry(today).first() })
    }

    /** Waits for a node matching [matcher]; Room loads data on background threads. */
    private fun waitForNode(matcher: SemanticsMatcher, timeoutMillis: Long = 5_000) {
        compose.waitUntilAtLeastOneExists(matcher, timeoutMillis)
    }
}
