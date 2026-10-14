package com.example.tracker.ui.photos

import com.example.tracker.domain.model.PhotoPose
import com.example.tracker.domain.photos.PhotoCheckIn
import com.example.tracker.domain.photos.PhotoComparison

/** Everything the Progress photos screen renders. */
sealed interface ProgressPhotosUiState {
    data object Loading : ProgressPhotosUiState
    data object Error : ProgressPhotosUiState

    data class Success(
        /** Newest day first; empty when no photo has been taken yet. */
        val checkIns: List<PhotoCheckIn>,
        val comparePose: PhotoPose,
        /** Null until [comparePose] was photographed on two days. */
        val comparison: PhotoComparison?,
    ) : ProgressPhotosUiState
}
