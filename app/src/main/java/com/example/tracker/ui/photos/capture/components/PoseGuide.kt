package com.example.tracker.ui.photos.capture.components

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.example.tracker.R
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

/** Body landmarks, as fractions of body height from the top of the head. */
private const val SHOULDER_LEVEL = 0.19f
private const val WAIST_LEVEL = 0.38f
private const val HIP_LEVEL = 0.5f
private const val KNEE_LEVEL = 0.72f

/** How far the level ticks reach in from each side, as a fraction of the frame width. */
private const val TICK_LENGTH = 0.14f

/**
 * A dashed human outline for [pose] between a head line and a feet line, with a centre line
 * and labelled ticks at shoulder, waist, hip and knee height. Standing inside it puts the
 * body at the same size, place and posture in every photo.
 */
@Composable
fun PoseGuide(pose: PhotoPose, modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(
        color = OverlayContent,
        shadow = Shadow(color = Color.Black.copy(alpha = 0.6f), blurRadius = 4f),
    )
    val levels = listOf(
        SHOULDER_LEVEL to stringResource(R.string.capture_level_shoulders),
        WAIST_LEVEL to stringResource(R.string.capture_level_waist),
        HIP_LEVEL to stringResource(R.string.capture_level_hips),
        KNEE_LEVEL to stringResource(R.string.capture_level_knees),
    )
    Canvas(modifier = modifier) {
        val top = size.height * HEAD_LINE
        val bottom = size.height * FEET_LINE
        val bodyHeight = bottom - top
        val fineDash = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 6.dp.toPx()))

        val guides = Path().apply {
            moveTo(0f, top)
            lineTo(size.width, top)
            moveTo(0f, bottom)
            lineTo(size.width, bottom)
            moveTo(size.width / 2, 0f)
            lineTo(size.width / 2, size.height)
            levels.forEach { (level, _) ->
                val y = top + level * bodyHeight
                moveTo(0f, y)
                lineTo(size.width * TICK_LENGTH, y)
                moveTo(size.width * (1 - TICK_LENGTH), y)
                lineTo(size.width, y)
            }
        }
        drawWithHalo(guides, 1.dp.toPx(), fineDash)

        val outline = poseOutline(pose, size, top, bottom)
        val dash = PathEffect.dashPathEffect(floatArrayOf(12.dp.toPx(), 8.dp.toPx()))
        drawWithHalo(outline.body, 2.dp.toPx(), dash)
        outline.detail?.let { drawWithHalo(it, 1.5.dp.toPx(), dash) }

        val inset = 4.dp.toPx()
        levels.forEach { (level, label) ->
            val layout = textMeasurer.measure(label, labelStyle)
            val y = top + level * bodyHeight
            drawText(layout, topLeft = Offset(inset, y - layout.size.height - 2.dp.toPx()))
        }
    }
}

private fun DrawScope.drawWithHalo(path: Path, width: Float, effect: PathEffect) {
    drawPath(path, OverlayScrim, style = Stroke(width = width * 3, pathEffect = effect))
    drawPath(path, OverlayContent, style = Stroke(width = width, pathEffect = effect))
}

/** The figure's outline, and for the side pose the arm drawn over the body. */
internal class PoseOutline(val body: Path, val detail: Path?)

/**
 * A standing human figure for [pose], in units of body height [top]..[bottom] around the
 * frame's centre line. Front and back share a figure with the arms a little away from the
 * body; the side view faces right, with the arm hanging along it.
 */
internal fun poseOutline(pose: PhotoPose, size: Size, top: Float, bottom: Float): PoseOutline {
    val u = bottom - top
    val cx = size.width / 2
    fun point(x: Float, y: Float) = Offset(cx + x * u, top + y * u)
    fun head(centerX: Float, halfWidth: Float) =
        Path().apply { addOval(Rect(point(centerX - halfWidth, 0f), point(centerX + halfWidth, HEAD_HEIGHT))) }

    return when (pose) {
        PhotoPose.FRONT, PhotoPose.BACK -> {
            val right = FRONT_RIGHT_HALF.map { (x, y) -> point(x, y) }
            val left = FRONT_RIGHT_HALF.reversed().drop(1).map { (x, y) -> point(-x, y) }
            PoseOutline(union(head(0f, FRONT_HEAD_HALF_WIDTH), smoothClosedPath(right + left)), detail = null)
        }

        PhotoPose.SIDE -> PoseOutline(
            body = union(head(SIDE_HEAD_OFFSET, SIDE_HEAD_HALF_WIDTH), smoothClosedPath(SIDE_BODY.map { (x, y) -> point(x, y) })),
            detail = smoothClosedPath(SIDE_ARM.map { (x, y) -> point(x, y) }),
        )
    }
}

private const val HEAD_HEIGHT = 0.13f
private const val FRONT_HEAD_HALF_WIDTH = 0.048f
private const val SIDE_HEAD_HALF_WIDTH = 0.052f

/** The side view's head sits slightly forward of the body's centre line. */
private const val SIDE_HEAD_OFFSET = 0.008f

/** Catmull-Rom to Bézier: each control point is a sixth of the neighbouring tangent away. */
private const val CATMULL_ROM_TANGENT_DIVISOR = 6f

private fun union(a: Path, b: Path) = Path().apply { op(a, b, PathOperation.Union) }

/** A closed Catmull-Rom curve through [points], so the outline bends smoothly through each one. */
private fun smoothClosedPath(points: List<Offset>): Path {
    val n = points.size
    return Path().apply {
        moveTo(points[0].x, points[0].y)
        for (i in 0 until n) {
            val p0 = points[(i - 1 + n) % n]
            val p1 = points[i]
            val p2 = points[(i + 1) % n]
            val p3 = points[(i + 2) % n]
            val c1 = p1 + (p2 - p0) / CATMULL_ROM_TANGENT_DIVISOR
            val c2 = p2 - (p3 - p1) / CATMULL_ROM_TANGENT_DIVISOR
            cubicTo(c1.x, c1.y, c2.x, c2.y, p2.x, p2.y)
        }
        close()
    }
}

/*
 * Outline points as (x, y): x from the centre line, y down from the top of the head, both in
 * body heights. Proportions follow a typical adult: head about 1/7.5 of the height, shoulders
 * about a quarter of it wide, the waist narrowest just above the navel, knees at about 0.72.
 */

/** The right half of the front figure, from the neck down the arm, torso and leg to the crotch. */
private val FRONT_RIGHT_HALF = listOf(
    0.03f to 0.10f, // neck, inside the head
    0.04f to 0.155f, // base of the neck
    0.09f to 0.175f, // trapezius
    0.13f to 0.195f, // shoulder
    0.145f to 0.235f, // deltoid
    0.15f to 0.30f,
    0.155f to 0.38f, // elbow
    0.162f to 0.45f,
    0.165f to 0.505f, // wrist
    0.17f to 0.555f,
    0.155f to 0.605f, // fingertips
    0.135f to 0.575f,
    0.133f to 0.505f,
    0.128f to 0.44f,
    0.123f to 0.38f,
    0.118f to 0.30f,
    0.105f to 0.25f, // armpit
    0.102f to 0.29f, // chest
    0.085f to WAIST_LEVEL,
    0.105f to 0.48f, // hip
    0.10f to 0.56f, // outer thigh
    0.068f to KNEE_LEVEL,
    0.062f to 0.80f, // calf
    0.038f to 0.95f, // ankle
    0.05f to 0.99f,
    0.04f to 1f, // toes
    0.012f to 0.995f,
    0.02f to 0.95f,
    0.02f to 0.82f,
    0.022f to KNEE_LEVEL,
    0.015f to 0.60f, // inner thigh
    0f to 0.525f, // crotch
)

/** The side figure facing right, from the back of the neck down the back and up the front. */
private val SIDE_BODY = listOf(
    -0.03f to 0.10f,
    -0.055f to 0.20f, // upper back
    -0.065f to 0.26f, // shoulder blade
    -0.05f to 0.34f,
    -0.045f to 0.40f, // lower back
    -0.06f to 0.46f,
    -0.072f to HIP_LEVEL, // seat
    -0.062f to 0.56f,
    -0.058f to 0.62f,
    -0.045f to KNEE_LEVEL,
    -0.058f to 0.79f, // calf
    -0.03f to 0.94f, // Achilles
    -0.035f to 0.99f, // heel
    -0.03f to 1f,
    0.075f to 1f, // toes
    0.07f to 0.985f,
    0.025f to 0.955f, // ankle
    0.032f to 0.82f, // shin
    0.045f to KNEE_LEVEL,
    0.065f to 0.60f, // thigh
    0.06f to HIP_LEVEL,
    0.06f to 0.42f, // belly
    0.05f to WAIST_LEVEL,
    0.065f to 0.31f,
    0.082f to 0.27f, // chest
    0.06f to 0.21f,
    0.035f to 0.17f, // throat
    0.03f to 0.10f,
)

/** The arm hanging along the side figure, drawn over the body so its position shows. */
private val SIDE_ARM = listOf(
    0f to 0.185f, // over the shoulder
    -0.025f to 0.20f,
    -0.028f to 0.30f,
    -0.024f to 0.38f, // elbow
    -0.016f to 0.47f,
    -0.01f to 0.52f, // wrist
    -0.012f to 0.58f,
    0.005f to 0.60f, // fingertips
    0.02f to 0.57f,
    0.012f to 0.515f,
    0.016f to 0.45f,
    0.018f to 0.38f,
    0.022f to 0.30f, // biceps
    0.024f to 0.21f,
)
