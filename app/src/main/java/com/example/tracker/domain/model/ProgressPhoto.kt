package com.example.tracker.domain.model

import java.time.LocalDate

/** The three angles of a progress check-in, in the order they are taken. */
enum class PhotoPose { FRONT, SIDE, BACK }

/**
 * One progress photo, taken in the check-in [checkInId], which has at most one per [pose].
 * [filePath] is the picture on this device.
 */
data class ProgressPhoto(
    val id: String,
    val checkInId: String,
    val date: LocalDate,
    val pose: PhotoPose,
    val filePath: String,
)

/** A body measurement noted with a check-in, e.g. waist 84.5 cm; [unit] may be empty. */
data class PhotoMeasurement(
    val name: String,
    val unit: String,
    val value: Double,
) {
    companion object {
        const val MAX_NAME_LENGTH = 30
        const val MAX_UNIT_LENGTH = 10

        /** Bounds that catch typos without limiting any real measurement. */
        const val MIN_VALUE = 0.0
        const val MAX_VALUE = 10_000.0
    }
}
