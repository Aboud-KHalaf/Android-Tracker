package com.example.tracker.ui.photos.capture.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.tracker.domain.model.PhotoPose

/*
 * Overlays sit on the live camera image, not on theme surfaces, so they use fixed light content
 * with a dark halo or scrim: that stays visible on any picture, in light and dark themes alike.
 */
internal val OverlayContent = Color.White.copy(alpha = 0.9f)
internal val OverlayScrim = Color.Black.copy(alpha = 0.35f)

/** Where the top of the head and the soles of the feet go, as fractions of the frame height. */
private const val HEAD_LINE = 0.07f
private const val FEET_LINE = 0.95f

/**
 * A dashed body outline for [pose] between a head line and a feet line, with a centre line.
 * Standing inside it puts the body at the same size and place in every photo.
 */
@Composable
fun PoseGuide(pose: PhotoPose, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val top = size.height * HEAD_LINE
        val bottom = size.height * FEET_LINE
        val dash = PathEffect.dashPathEffect(floatArrayOf(12.dp.toPx(), 8.dp.toPx()))

        val guides = Path().apply {
            moveTo(0f, top)
            lineTo(size.width, top)
            moveTo(0f, bottom)
            lineTo(size.width, bottom)
            moveTo(size.width / 2, 0f)
            lineTo(size.width / 2, size.height)
        }
        drawWithHalo(guides, 1.dp.toPx(), PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 6.dp.toPx())))
        drawWithHalo(bodyOutline(pose, size, top, bottom), 2.dp.toPx(), dash)
    }
}

private fun DrawScope.drawWithHalo(path: Path, width: Float, effect: PathEffect) {
    drawPath(path, OverlayScrim, style = Stroke(width = width * 3, pathEffect = effect))
    drawPath(path, OverlayContent, style = Stroke(width = width, pathEffect = effect))
}

/**
 * A simple standing figure, built from rounded shapes in units of body height [top]..[bottom]
 * and merged into one outline. Front and back share a shape; side is narrower.
 */
private fun bodyOutline(pose: PhotoPose, size: Size, top: Float, bottom: Float): Path {
    val u = bottom - top
    val cx = size.width / 2

    fun capsule(left: Float, topY: Float, right: Float, bottomY: Float, radius: Float = (right - left) / 2) =
        Path().apply {
            addRoundRect(
                RoundRect(Rect(cx + left * u, top + topY * u, cx + right * u, top + bottomY * u), CornerRadius(radius * u)),
            )
        }

    fun oval(halfWidth: Float, topY: Float, bottomY: Float) = Path().apply {
        addOval(Rect(Offset(cx - halfWidth * u, top + topY * u), Size(2 * halfWidth * u, (bottomY - topY) * u)))
    }

    val parts = when (pose) {
        PhotoPose.FRONT, PhotoPose.BACK -> listOf(
            oval(halfWidth = 0.05f, topY = 0f, bottomY = 0.13f),
            capsule(-0.025f, 0.11f, 0.025f, 0.18f),
            capsule(-0.12f, 0.16f, 0.12f, 0.53f, radius = 0.05f),
            capsule(-0.17f, 0.17f, -0.125f, 0.5f),
            capsule(0.125f, 0.17f, 0.17f, 0.5f),
            capsule(-0.105f, 0.47f, -0.01f, 1f),
            capsule(0.01f, 0.47f, 0.105f, 1f),
        )

        PhotoPose.SIDE -> listOf(
            oval(halfWidth = 0.055f, topY = 0f, bottomY = 0.13f),
            capsule(-0.025f, 0.11f, 0.025f, 0.18f),
            capsule(-0.075f, 0.16f, 0.08f, 0.53f, radius = 0.05f),
            capsule(-0.065f, 0.47f, 0.065f, 1f, radius = 0.05f),
        )
    }
    return parts.reduce { outline, part -> Path().apply { op(outline, part, PathOperation.Union) } }
}
