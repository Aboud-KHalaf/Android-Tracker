package com.example.tracker.ui

import android.Manifest
import android.app.Application
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import com.example.tracker.MainActivity
import com.example.tracker.TrackerApplication
import com.example.tracker.data.repository.newDay
import com.example.tracker.domain.model.ExerciseConfig
import com.example.tracker.domain.model.ExerciseType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * The app's critical flow end to end, with the real database, navigation and ViewModels:
 * start a workout from Home, log a set, finish, and find it in History.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
// A typical phone screen; the default is too small for the active-set card.
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class WorkoutFlowTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Before
    fun seedPlan(): Unit = runBlocking {
        // Starting a workout asks for this; Robolectric can't answer the system prompt.
        shadowOf(ApplicationProvider.getApplicationContext<Application>())
            .grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val container = ApplicationProvider.getApplicationContext<TrackerApplication>().container
        val planId = container.programRepository.newDay("Push Day")
        val benchId = container.exerciseRepository.createExercise("Bench Press", ExerciseType.WEIGHT_REPS)
        container.planRepository.addExercise(planId, benchId, ExerciseConfig(targetSets = 2))
        // Wait until the plan is readable, so Home's first load includes it.
        container.planRepository.observePlans().first { plans -> plans.any { it.exerciseCount == 1 } }
        Unit
    }

    @Test
    fun startLogAndFinishWorkout_showsItInHistory() {
        // Home suggests the plan.
        waitForNode(hasText("Start workout"))
        compose.onNodeWithText("Start workout").performClick()

        // Active workout: a first-time exercise starts empty, so the set can't be completed yet.
        waitForNode(hasText("First time doing this exercise"))
        compose.onNodeWithText("Complete set").assertIsNotEnabled()

        compose.onNodeWithContentDescription("Weight (kg)").performTextReplacement("50")
        compose.onNodeWithContentDescription("Reps").performTextReplacement("7")
        compose.onNodeWithContentDescription("Increase reps by 1").performClick()
        compose.onNodeWithText("Complete set").performScrollTo().assertIsEnabled().performClick()

        waitForNode(hasContentDescription("Set 1 done, 50 kg × 8"))
        waitForNode(hasText("Set 1 saved"))

        // Finishing with an open set asks first.
        compose.onNodeWithText("Finish").performClick()
        waitForNode(hasText("1 set isn't completed and won't be saved."))
        compose.onNode(hasText("Finish") and hasAnyAncestor(isDialog())).performClick()

        // Its summary follows, with the set that was saved; Done goes back Home.
        waitForNode(hasText("Workout done. Nice work!"))
        compose.onNodeWithText("50×8 kg", substring = true).assertExists()
        compose.onNodeWithText("Done").performClick()

        // Back on Home, the workout counts toward this week.
        waitForNode(hasText("1 workout", substring = true))

        // History, opened from the Home header, lists it: the row's "Sat · 1 exercise · 0 min".
        compose.onNodeWithContentDescription("Workout history").performClick()
        waitForNode(hasText("History"))
        waitForNode(hasText("1 exercise", substring = true))
        compose.onNodeWithText("Finished workouts will show up here.").assertDoesNotExist()

        // Tapping it opens its summary again, without the "done" headline.
        compose.onNodeWithText("1 exercise", substring = true).performClick()
        waitForNode(hasText("Exercises"))
        compose.onNodeWithText("Workout done. Nice work!").assertDoesNotExist()
    }

    /** Waits for a node matching [matcher]; Room loads data on background threads. */
    private fun waitForNode(matcher: SemanticsMatcher, timeoutMillis: Long = 5_000) {
        compose.waitUntilAtLeastOneExists(matcher, timeoutMillis)
    }
}
