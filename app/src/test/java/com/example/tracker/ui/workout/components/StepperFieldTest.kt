package com.example.tracker.ui.workout.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.input.KeyboardType
import com.example.tracker.ui.theme.TrackerTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class StepperFieldTest {

    @get:Rule
    val compose = createComposeRule()

    private var value by mutableStateOf("50")
    private var steps = 0

    private fun setContent() {
        compose.setContent {
            TrackerTheme {
                StepperField(
                    label = "Weight",
                    value = value,
                    isValid = true,
                    keyboardType = KeyboardType.Decimal,
                    decreaseDescription = "Less",
                    increaseDescription = "More",
                    onValueChange = { value = it },
                    onDecrease = { steps--; value = (value.toInt() - 1).toString() },
                    onIncrease = { steps++; value = (value.toInt() + 1).toString() },
                )
            }
        }
    }

    @Test
    fun typingAfterFocus_replacesTheNumber() {
        setContent()

        compose.onNodeWithContentDescription("Weight").performClick()
        compose.onNodeWithContentDescription("Weight").performTextInput("62")

        assertEquals("62", value)
    }

    @Test
    fun stepping_showsTheNewValueEvenWhileFocused() {
        setContent()
        compose.onNodeWithContentDescription("Weight").performClick()

        compose.onNodeWithContentDescription("More").performClick()

        assertEquals("51", value)
        compose.onNodeWithContentDescription("Weight").assertTextEquals("51")
    }

    @Test
    fun holdingStep_repeats() {
        setContent()

        compose.onNodeWithContentDescription("More").performTouchInput {
            down(center)
            advanceEventTime(1_000)
            up()
        }
        compose.waitForIdle()

        assertTrue("Expected repeated steps, got $steps", steps > 2)
    }
}
