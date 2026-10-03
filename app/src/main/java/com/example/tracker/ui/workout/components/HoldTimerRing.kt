package com.example.tracker.ui.workout.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.example.tracker.ui.theme.tabularNumbers

private val RingSize = 176.dp
private val RingStroke = 10.dp
private const val START_ANGLE = -90f
private const val FULL_CIRCLE = 360f

/**
 * The hold's time inside a ring that fills toward last time's hold.
 * [progress] is 0..1; [statusColor] highlights going past the target.
 */
@Composable
fun HoldTimerRing(
    timeText: String,
    statusText: String,
    progress: Float,
    statusColor: Color,
    modifier: Modifier = Modifier,
) {
    val animatedProgress by animateFloatAsState(targetValue = progress.coerceIn(0f, 1f), label = "holdProgress")
    val trackColor = MaterialTheme.colorScheme.outlineVariant
    val progressColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .size(RingSize)
            .clearAndSetSemantics { contentDescription = "$timeText, $statusText" },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(width = RingStroke.toPx(), cap = StrokeCap.Round)
            val inset = stroke.width / 2
            val arcSize = size.copy(width = size.width - stroke.width, height = size.height - stroke.width)
            val topLeft = Offset(inset, inset)
            drawArc(trackColor, 0f, FULL_CIRCLE, useCenter = false, topLeft = topLeft, size = arcSize, style = stroke)
            if (animatedProgress > 0f) {
                drawArc(
                    color = progressColor,
                    startAngle = START_ANGLE,
                    sweepAngle = FULL_CIRCLE * animatedProgress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = stroke,
                )
            }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(timeText, style = MaterialTheme.typography.displayMedium.tabularNumbers())
            Text(statusText, style = MaterialTheme.typography.labelMedium.tabularNumbers(), color = statusColor)
        }
    }
}
