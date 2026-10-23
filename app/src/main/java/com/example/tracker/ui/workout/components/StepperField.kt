package com.example.tracker.ui.workout.components

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.tracker.ui.theme.Dimens
import com.example.tracker.ui.theme.spacing
import com.example.tracker.ui.theme.tabularNumbers
import kotlinx.coroutines.delay

private val InvalidBorderWidth = 2.dp

/** How long −/+ must be held before it starts repeating, and how often it repeats then. */
private const val REPEAT_DELAY_MILLIS = 400L
private const val REPEAT_INTERVAL_MILLIS = 90L

/**
 * A big numeric field between −/+ buttons, e.g. "Weight (kg)  [−] 50 [+]".
 *
 * Focusing the field selects its number, so typing replaces it instead of adding to it.
 * What the user types is kept here while they type, so a slow round trip through the
 * ViewModel can never undo a keystroke; [value] replaces it when it changes from outside,
 * e.g. from −/+ or while the field isn't focused. Holding −/+ keeps stepping.
 * [onImeAction] runs on the keyboard's action key ([imeAction]); Done by default just hides it.
 */
@Composable
fun StepperField(
    label: String,
    value: String,
    isValid: Boolean,
    keyboardType: KeyboardType,
    decreaseDescription: String,
    increaseDescription: String,
    onValueChange: (String) -> Unit,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier,
    errorDescription: String = label,
    imeAction: ImeAction = ImeAction.Done,
    onImeAction: (() -> Unit)? = null,
    focusRequester: FocusRequester = remember { FocusRequester() },
) {
    val focusManager = LocalFocusManager.current
    var field by remember { mutableStateOf(TextFieldValue(value, TextRange(value.length))) }
    var isFocused by remember { mutableStateOf(false) }
    // Set by −/+ so their result shows even while the field has focus.
    var acceptOutside by remember { mutableStateOf(false) }
    // The first tap after focusing would place the cursor; keep the whole number selected instead.
    var keepSelectAll by remember { mutableStateOf(false) }

    if (value != field.text && (!isFocused || acceptOutside)) {
        field = TextFieldValue(value, TextRange(value.length))
        acceptOutside = false
    }

    val runImeAction = { onImeAction?.invoke() ?: focusManager.clearFocus() }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
        ) {
            StepButton(Icons.Outlined.Remove, decreaseDescription) {
                acceptOutside = true
                onDecrease()
            }
            BasicTextField(
                value = field,
                onValueChange = { changed ->
                    val textChanged = changed.text != field.text
                    field = if (keepSelectAll && !textChanged) {
                        changed.copy(selection = TextRange(0, changed.text.length))
                    } else {
                        changed
                    }
                    keepSelectAll = false
                    if (textChanged) {
                        acceptOutside = false
                        onValueChange(changed.text)
                    }
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.displaySmall.tabularNumbers().copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
                keyboardActions = KeyboardActions(onAny = { runImeAction() }),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .onFocusChanged { state ->
                        if (state.isFocused && !isFocused) {
                            field = field.copy(selection = TextRange(0, field.text.length))
                            keepSelectAll = true
                        }
                        isFocused = state.isFocused
                    }
                    .semantics {
                        contentDescription = label
                        if (!isValid) error(errorDescription)
                    },
                decorationBox = { innerTextField ->
                    val shape = MaterialTheme.shapes.medium
                    Box(
                        modifier = Modifier
                            .height(Dimens.setControlSize)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest, shape)
                            .then(
                                if (isValid) Modifier else Modifier.border(InvalidBorderWidth, MaterialTheme.colorScheme.error, shape),
                            ),
                        contentAlignment = Alignment.Center,
                    ) { innerTextField() }
                },
            )
            StepButton(Icons.Outlined.Add, increaseDescription) {
                acceptOutside = true
                onIncrease()
            }
        }
    }
}

/** A −/+ button that steps once per tap and keeps stepping while held. */
@Composable
private fun StepButton(icon: ImageVector, description: String, onStep: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val currentOnStep by rememberUpdatedState(onStep)
    // A hold that already repeated shouldn't step once more on release.
    var repeated by remember { mutableStateOf(false) }
    LaunchedEffect(isPressed) {
        if (!isPressed) return@LaunchedEffect
        repeated = false
        delay(REPEAT_DELAY_MILLIS)
        repeated = true
        while (true) {
            currentOnStep()
            delay(REPEAT_INTERVAL_MILLIS)
        }
    }
    FilledTonalIconButton(
        onClick = {
            if (repeated) repeated = false else currentOnStep()
        },
        shape = MaterialTheme.shapes.large,
        interactionSource = interactionSource,
        modifier = Modifier.size(Dimens.setControlSize),
    ) {
        Icon(icon, contentDescription = description)
    }
}
