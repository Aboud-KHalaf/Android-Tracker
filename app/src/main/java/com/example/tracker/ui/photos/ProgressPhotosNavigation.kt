package com.example.tracker.ui.photos

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.tracker.ui.photos.capture.PhotoCaptureRoute
import kotlinx.serialization.Serializable

@Serializable
data object ProgressPhotosDestination

@Serializable
data object PhotoCaptureDestination

fun NavController.navigateToProgressPhotos() = navigate(ProgressPhotosDestination) { launchSingleTop = true }

fun NavController.navigateToPhotoCapture() = navigate(PhotoCaptureDestination) { launchSingleTop = true }

fun NavGraphBuilder.progressPhotosScreen(onBack: () -> Unit, onTakePhotos: () -> Unit) {
    composable<ProgressPhotosDestination> {
        ProgressPhotosRoute(onBack = onBack, onTakePhotos = onTakePhotos)
    }
}

fun NavGraphBuilder.photoCaptureScreen(onClose: () -> Unit) {
    composable<PhotoCaptureDestination> {
        PhotoCaptureRoute(onClose = onClose)
    }
}
