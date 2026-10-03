package com.example.tracker.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.tracker.MainActivity
import com.example.tracker.TrackerApplication
import com.example.tracker.domain.model.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Opening Settings from Home, switching to the dark theme, and deleting all data. */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class SettingsFlowTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val container get() = ApplicationProvider.getApplicationContext<TrackerApplication>().container

    @Before
    fun seedPlan(): Unit = runBlocking {
        container.planRepository.createPlan("Push Day")
        container.planRepository.observePlans().first { it.isNotEmpty() }
        Unit
    }

    @Test
    fun changeThemeAndDeleteAllData_returnsHomeToEmpty() {
        waitForNode(hasText("Push Day"))
        compose.onNodeWithContentDescription("Settings").performClick()

        compose.onNodeWithText("Dark").performClick()
        waitForNode(hasText("Dark") and isSelected())
        assertEquals(ThemeMode.DARK, container.settingsRepository.themeMode.value)

        // Deleting asks first.
        compose.onNodeWithText("Delete all data").performClick()
        compose.onNode(hasText("Delete all") and hasAnyAncestor(isDialog())).performClick()
        waitForNode(hasText("All data deleted"))

        compose.onNodeWithContentDescription("Back").performClick()
        waitForNode(hasText("No plans yet. Create one to get started."))
        compose.onNodeWithText("Push Day").assertDoesNotExist()
    }

    /** Waits for a node matching [matcher]; Room loads data on background threads. */
    private fun waitForNode(matcher: SemanticsMatcher, timeoutMillis: Long = 5_000) {
        compose.waitUntilAtLeastOneExists(matcher, timeoutMillis)
    }
}
