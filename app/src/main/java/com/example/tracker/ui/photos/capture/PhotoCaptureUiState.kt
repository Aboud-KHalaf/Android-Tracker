package com.example.tracker.ui.photos.capture

import com.example.tracker.domain.model.PhotoPose

/** Self-timer lengths, so the phone can be propped up and the user can step back. */
enum class CaptureTimer(val seconds: Int) {
    OFF(0),
    THREE(3),
    TEN(10);

    fun next(): CaptureTimer = entries[(ordinal + 1) % entries.size]
}

/** Everything the capture screen renders. */
data class PhotoCaptureUiState(
    val pose: PhotoPose = PhotoPose.FRONT,
    /** Poses already photographed today; retaking one replaces it. */
    val takenToday: Set<PhotoPose> = emptySet(),
    /** The last photo of [pose], shown faintly over the camera to line up with; null when there is none. */
    val guidePhotoPath: String? = null,
    val showGuidePhoto: Boolean = true,
    val useFrontCamera: Boolean = false,
    val timer: CaptureTimer = CaptureTimer.TEN,
    /** Seconds left on the running self-timer; null when it isn't running. */
    val countdown: Int? = null,
    val isTakingPicture: Boolean = false,
    /** The picture just taken, waiting to be kept or retaken; null while the camera is showing. */
    val reviewPath: String? = null,
    val isSaving: Boolean = false,
) {
    /** 1-based position of [pose] in the front, side, back sequence. */
    val step: Int get() = pose.ordinal + 1

    /** Whether the shutter can start a new photo. */
    val canShoot: Boolean get() = !isTakingPicture && reviewPath == null && !isSaving
}
