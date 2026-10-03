package com.example.tracker.ui.common

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import com.example.tracker.ui.theme.Motion

private data class RollingValue(val value: Double, val text: String)

/**
 * [text] showing a number that rolls like an odometer when it changes: up when [value]
 * grows, down when it shrinks.
 */
@Composable
fun RollingNumberText(
    value: Double,
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = RollingValue(value, text),
        modifier = modifier,
        transitionSpec = {
            val direction = if (targetState.value >= initialState.value) 1 else -1
            val slide = tween<IntOffset>(Motion.DurationMedium, easing = Motion.Emphasized)
            (slideInVertically(slide) { it * direction } + fadeIn(tween(Motion.DurationMedium))) togetherWith
                (slideOutVertically(slide) { -it * direction } + fadeOut(tween(Motion.DurationShort))) using
                SizeTransform(clip = true)
        },
        contentKey = { it.text },
        label = "rollingNumber",
    ) { Text(it.text, style = style) }
}
