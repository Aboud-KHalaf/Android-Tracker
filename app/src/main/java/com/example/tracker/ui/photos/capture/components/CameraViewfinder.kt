package com.example.tracker.ui.photos.capture.components

import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * The live camera image, filling and cropped to this composable's bounds. [controller] crops
 * its pictures to the same bounds, so a photo shows exactly what the viewfinder showed.
 */
@Composable
fun CameraViewfinder(controller: LifecycleCameraController, modifier: Modifier = Modifier) {
    AndroidView(
        factory = { context ->
            PreviewView(context).apply {
                // A TextureView, so the image clips to rounded corners and sits under Compose overlays.
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                scaleType = PreviewView.ScaleType.FILL_CENTER
                this.controller = controller
            }
        },
        onRelease = { it.controller = null },
        modifier = modifier,
    )
}
