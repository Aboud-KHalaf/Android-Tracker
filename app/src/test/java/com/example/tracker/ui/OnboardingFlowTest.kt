package com.example.tracker.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.tracker.MainActivity
import com.example.tracker.TrackerApplication
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** First launch of an empty app: onboarding sets up a program and a stat, then opens the program. */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class OnboardingFlowTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val container get() = ApplicationProvider.getApplicationContext<TrackerApplication>().container

    @Test
    fun firstLaunch_setsUpAProgramAndAStat() {
        waitForNode(hasText("Welcome to Tracker"))
        compose.onNodeWithText("Get started").performScrollTo().performClick()

        waitForNode(hasText("Plan your training"))
        compose.onNodeWithText("Create program").performScrollTo().performClick()

        waitForNode(hasText("What else will you track?"))
        compose.onNodeWithText("Start using Tracker").performScrollTo().performClick()

        // The new program opens, ready for exercises.
        waitForNode(hasText("Full Body"))
        assertTrue(container.settingsRepository.onboardingDone.value)
        val stats = runBlocking { container.customStatRepository.observeOverviews().first { it.isNotEmpty() } }
        assertEquals(listOf("Weight"), stats.map { it.stat.name })
    }

    /** Waits for a node matching [matcher]; Room loads data on background threads. */
    private fun waitForNode(matcher: SemanticsMatcher, timeoutMillis: Long = 5_000) {
        compose.waitUntilAtLeastOneExists(matcher, timeoutMillis)
    }
}
