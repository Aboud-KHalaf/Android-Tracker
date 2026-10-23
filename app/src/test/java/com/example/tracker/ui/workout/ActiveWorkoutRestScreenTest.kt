package com.example.tracker.ui.workout

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.tracker.domain.model.ExerciseType
import com.example.tracker.domain.workout.RestKind
import com.example.tracker.ui.common.SetValueUi
import com.example.tracker.ui.theme.TrackerTheme
import com.example.tracker.ui.workout.components.RestTimerActions
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class ActiveWorkoutRestScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private fun state(kind: RestKind) = ActiveWorkoutUiState.Success(
        workoutName = "Push Day",
        startedAt = Instant.now().minusSeconds(600),
        exerciseIndex = 0,
        exerciseCount = 2,
        exercise = CurrentExerciseUi("we", "bench", "Bench Press", ExerciseType.WEIGHT_REPS, lastTime = emptyList()),
        sets = listOf(
            SetRowUi.Done("s1", 1, SetValueUi.WeightReps(50.0, 8), improvement = null),
            SetRowUi.Active("s2", 2, SetValueUi.WeightReps(50.0, 8), SetEditorUi.WeightReps("50", "8")),
        ),
        previousExerciseName = null,
        nextExerciseName = "Incline Press",
        nextOpenExercise = NextExerciseUi(1, "Incline Press"),
        completedSetCount = 1,
        openSetCount = 3,
        rest = RestUi(kind, Instant.now().plusSeconds(90), 90),
    )

    @Test
    fun rest_showsOnItsOwnScreenWithTheNextSet() {
        var skipped = 0
        compose.setContent {
            TrackerTheme {
                ActiveWorkoutScreen(
                    uiState = state(RestKind.BETWEEN_SETS),
                    actions = ActiveWorkoutActions(rest = RestTimerActions(onEnd = { skipped++ })),
                )
            }
        }

        compose.onNodeWithText("Next set in").assertExists()
        compose.onNodeWithText("Set 2 · Last time 50 kg × 8").assertExists()
        compose.onNodeWithContentDescription("Weight (kg)").assertDoesNotExist()
        compose.onNodeWithText("Skip rest").performClick()

        assertEquals(1, skipped)
    }

    @Test
    fun showWorkout_shrinksTheRestToABarAndBack() {
        compose.setContent {
            TrackerTheme { ActiveWorkoutScreen(uiState = state(RestKind.BETWEEN_SETS), actions = ActiveWorkoutActions()) }
        }

        compose.onNodeWithText("Show workout").performClick()
        compose.waitForIdle()

        compose.onNodeWithText("Skip rest").assertDoesNotExist()
        compose.onNodeWithContentDescription("Weight (kg)").assertExists()
        compose.onNodeWithText("Next set in", useUnmergedTree = true).performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Skip rest").assertExists()
    }

    @Test
    fun restBetweenExercises_showsTheNextExercise() {
        val done = state(RestKind.BETWEEN_EXERCISES).let { it.copy(sets = it.sets.take(1)) }
        compose.setContent {
            TrackerTheme { ActiveWorkoutScreen(uiState = done, actions = ActiveWorkoutActions()) }
        }

        compose.onNodeWithText("Next exercise in").assertExists()
        compose.onNodeWithText("Incline Press").assertExists()
    }
}
