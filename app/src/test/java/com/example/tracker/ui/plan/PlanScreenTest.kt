package com.example.tracker.ui.plan

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import com.example.tracker.domain.model.ExerciseConfig
import com.example.tracker.domain.model.ExerciseType
import com.example.tracker.ui.theme.TrackerTheme
import java.time.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class PlanScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private fun exercise(index: Int, name: String, count: Int) = PlanExerciseUi(
        id = "pe$index",
        exerciseId = "e$index",
        name = name,
        type = ExerciseType.WEIGHT_REPS,
        config = ExerciseConfig(),
        canMoveUp = index > 0,
        canMoveDown = index < count - 1,
    )

    private val state = PlanUiState.Success(
        name = "Push",
        dayOfWeek = DayOfWeek.MONDAY,
        lastDoneOn = null,
        exercises = listOf("Bench Press", "Overhead Press", "Dips").mapIndexed { i, name -> exercise(i, name, 3) },
        availableExercises = listOf(ExerciseOptionUi("db", "Dumbbell Press", ExerciseType.WEIGHT_REPS)),
        workoutAction = WorkoutActionUi.Start,
    )

    @Test
    fun draggingTheHandleDown_movesTheExerciseOnDrop() {
        val moves = mutableListOf<Pair<Int, Int>>()
        compose.setContent {
            TrackerTheme {
                PlanScreen(uiState = state, actions = PlanActions(onMoveExercise = { from, to -> moves += from to to }))
            }
        }

        // The row merges its children's semantics, so look up the handle itself.
        val handle = compose.onNodeWithContentDescription("Reorder Bench Press", useUnmergedTree = true)
        handle.performTouchInput { down(center) }
        // Two rows down in small steps, with frames in between as on a phone.
        repeat(STEPS) { handle.performTouchInput { moveBy(Offset(0f, height * STEP_FRACTION)) } }
        handle.performTouchInput { up() }

        assertEquals(listOf(0 to 2), moves)
    }

    @Test
    fun touchEventsFasterThanFrames_doNotUndoTheSwap() {
        val moves = mutableListOf<Pair<Int, Int>>()
        compose.setContent {
            TrackerTheme {
                PlanScreen(uiState = state, actions = PlanActions(onMoveExercise = { from, to -> moves += from to to }))
            }
        }

        // Every move is sent before the list is laid out again.
        compose.onNodeWithContentDescription("Reorder Bench Press", useUnmergedTree = true).performTouchInput {
            down(center)
            repeat(STEPS) { moveBy(Offset(0f, height * STEP_FRACTION)) }
            up()
        }

        assertEquals(listOf(0 to 1), moves)
    }

    @Test
    fun replace_picksALibraryExerciseForThatRow() {
        val replaced = mutableListOf<Pair<String, String>>()
        compose.setContent {
            TrackerTheme {
                PlanScreen(
                    uiState = state,
                    actions = PlanActions(onReplaceExercise = { planExerciseId, exerciseId -> replaced += planExerciseId to exerciseId }),
                )
            }
        }

        compose.onNodeWithContentDescription("More options for Overhead Press").performClick()
        compose.onNodeWithText("Replace").performClick()
        compose.onNodeWithText("Replace Overhead Press").assertExists()
        compose.onNodeWithText("Dumbbell Press").performClick()

        assertEquals(listOf("pe1" to "db"), replaced)
    }

    @Test
    fun editTargets_savesTheTypedConfiguration() {
        val saved = mutableListOf<Pair<String, ExerciseConfig>>()
        val editing = state.copy(
            editor = ExerciseEditorUi("pe0", "Bench Press", ExerciseType.WEIGHT_REPS, ExerciseConfig(), lastRecorded = emptyList()),
        )
        compose.setContent {
            TrackerTheme {
                PlanScreen(
                    uiState = editing,
                    actions = PlanActions(onSaveExerciseConfig = { id, config -> saved += id to config }),
                )
            }
        }

        compose.onNodeWithContentDescription("More sets").performClick()
        compose.onNodeWithText("Target reps").performTextReplacement("8")
        compose.onNodeWithText("Target weight (kg)").performTextReplacement("60")
        compose.onNodeWithText("Not done yet", substring = true).assertExists()
        compose.onNodeWithText("Save").performClick()

        assertEquals(listOf("pe0" to ExerciseConfig(targetSets = 4, targetReps = 8, targetWeightKg = 60.0)), saved)
    }

    private companion object {
        const val STEPS = 30
        const val STEP_FRACTION = 0.12f
    }
}
