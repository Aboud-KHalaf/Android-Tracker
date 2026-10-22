package com.example.tracker.ui

import android.Manifest
import android.os.Build
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.example.tracker.MainActivity
import com.example.tracker.TrackerApplication
import com.example.tracker.domain.model.ExerciseConfig
import com.example.tracker.domain.model.ExerciseType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The critical journey on a real device: start a workout from Home, log a set, finish, and find
 * it in History. The Robolectric WorkoutFlowTest covers the same steps on every build; this one
 * runs them on a real Android runtime. It deletes the app's data first.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class WorkoutJourneyTest {

    // Starting a workout asks for notifications on Android 13+; a system dialog would block the test.
    @get:Rule
    val notifications: GrantPermissionRule =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            GrantPermissionRule.grant()
        }

    @get:Rule
    val compose = createEmptyComposeRule()

    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun startWithOnePlan(): Unit = runBlocking {
        val container = ApplicationProvider.getApplicationContext<TrackerApplication>().container
        container.settingsRepository.deleteAllData()
        val programId = container.programRepository.createProgram("Journey", emptyMap())
        val planId = container.programRepository.addDay(programId, dayOfWeek = null, name = "Push Day")
        val benchId = container.exerciseRepository.createExercise("Bench Press", ExerciseType.WEIGHT_REPS)
        container.planRepository.addExercise(planId, benchId, ExerciseConfig(targetSets = 2))
        container.planRepository.observePlans().first { plans -> plans.any { it.exerciseCount == 1 } }
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun closeApp() {
        scenario.close()
    }

    @Test
    fun startLogAndFinishWorkout_showsItInHistory() {
        waitForNode(hasText("Start workout"))
        compose.onNodeWithText("Start workout").performClick()

        waitForNode(hasText("First time doing this exercise"))
        compose.onNodeWithContentDescription("Weight (kg)").performTextReplacement("50")
        compose.onNodeWithContentDescription("Reps").performTextReplacement("8")
        compose.onNodeWithText("Complete set").performScrollTo().assertIsEnabled().performClick()
        waitForNode(hasContentDescription("Set 1 done, 50 kg × 8"))

        compose.onNodeWithText("Finish").performClick()
        waitForNode(hasText("Finish") and hasAnyAncestor(isDialog()))
        compose.onNode(hasText("Finish") and hasAnyAncestor(isDialog())).performClick()

        // The summary follows; Done returns Home.
        waitForNode(hasText("Workout done. Nice work!"))
        compose.onNodeWithText("Done").performClick()

        waitForNode(hasText("1 workout", substring = true))
        compose.onNodeWithContentDescription("Workout history").performClick()
        waitForNode(hasText("1 exercise", substring = true))
    }

    /** Waits for a node matching [matcher]; Room loads data on background threads. */
    private fun waitForNode(matcher: SemanticsMatcher, timeoutMillis: Long = 10_000) {
        compose.waitUntilAtLeastOneExists(matcher, timeoutMillis)
    }
}
