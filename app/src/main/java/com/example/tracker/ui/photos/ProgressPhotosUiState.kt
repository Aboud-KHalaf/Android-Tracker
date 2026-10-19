package com.example.tracker.ui.photos

import com.example.tracker.domain.model.PhotoMeasurement
import com.example.tracker.domain.model.PhotoPose
import com.example.tracker.domain.photos.PhotoCheckIn
import com.example.tracker.domain.photos.PhotoComparison

/** Everything the Progress photos screen renders. */
sealed interface ProgressPhotosUiState {
    data object Loading : ProgressPhotosUiState
    data object Error : ProgressPhotosUiState

    data class Success(
        /** Newest first; empty when no photo has been taken yet. */
        val checkIns: List<CheckInUi>,
        val comparePose: PhotoPose,
        /** Null until [comparePose] was photographed in two check-ins. */
        val comparison: PhotoComparison?,
    ) : ProgressPhotosUiState
}

/**
 * A check-in with how each measurement changed since the one before that has it, and
 * whether another check-in shares its day (so the time is shown to tell them apart).
 */
data class CheckInUi(
    val checkIn: PhotoCheckIn,
    val changes: Map<PhotoMeasurement, Double>,
    val sharesDay: Boolean,
)
