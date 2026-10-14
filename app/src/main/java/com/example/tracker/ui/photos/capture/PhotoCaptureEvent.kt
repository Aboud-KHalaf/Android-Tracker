package com.example.tracker.ui.photos.capture

import androidx.annotation.StringRes
import java.io.File

/** One-off effects the capture screen handles once. */
sealed interface PhotoCaptureEvent {
    /** Take a picture into [file], then report back. */
    data class TakePicture(val file: File) : PhotoCaptureEvent

    data class ShowMessage(@StringRes val messageRes: Int) : PhotoCaptureEvent

    /** All three poses are saved for today. */
    data object Finished : PhotoCaptureEvent
}
