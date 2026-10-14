package com.example.tracker.domain.model

import java.time.LocalDate

/** The three angles of a progress check-in, in the order they are taken. */
enum class PhotoPose { FRONT, SIDE, BACK }

/** One progress photo; a day has at most one per [pose]. [filePath] is the picture on this device. */
data class ProgressPhoto(
    val id: String,
    val date: LocalDate,
    val pose: PhotoPose,
    val filePath: String,
)
