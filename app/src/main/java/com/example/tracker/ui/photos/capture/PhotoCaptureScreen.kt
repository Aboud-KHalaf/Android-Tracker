package com.example.tracker.ui.photos.capture

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.tracker.R
import com.example.tracker.domain.model.PhotoPose
import com.example.tracker.ui.common.MessageContent
import com.example.tracker.ui.common.ObserveAsEvents
import com.example.tracker.ui.photos.capture.components.CameraControls
import com.example.tracker.ui.photos.capture.components.CameraViewfinder
import com.example.tracker.ui.photos.capture.components.CountdownBadge
import com.example.tracker.ui.photos.capture.components.PoseGuide
import com.example.tracker.ui.photos.capture.components.ReviewControls
import com.example.tracker.ui.photos.components.PHOTO_ASPECT_RATIO
import com.example.tracker.ui.photos.components.PoseSelector
import com.example.tracker.ui.photos.hintRes
import com.example.tracker.ui.photos.labelRes
import com.example.tracker.ui.photos.measurements.MeasurementsActions
import com.example.tracker.ui.photos.measurements.MeasurementsScreen
import com.example.tracker.ui.theme.Dimens
import com.example.tracker.ui.theme.spacing
import java.io.File
import kotlinx.coroutines.launch

/** How faint the last photo is over the camera: visible enough to line up with, faint enough to see through. */
private const val GUIDE_PHOTO_ALPHA = 0.4f

/** Whether the camera can be shown. */
enum class CameraStatus { NEEDS_PERMISSION, UNAVAILABLE, READY }

/**
 * Capture destination: asks for the camera, runs it with CameraX, and connects
 * [PhotoCaptureViewModel] to [PhotoCaptureScreen].
 */
@Composable
fun PhotoCaptureRoute(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PhotoCaptureViewModel = viewModel(factory = PhotoCaptureViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val permission = rememberCameraPermission()
    val hasPermission = permission.granted
    val controller = remember { createCameraController(context) }
    val available = rememberLenses(controller, hasPermission)
    SelectCamera(controller, available, useFrontCamera = uiState.useFrontCamera)

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is PhotoCaptureEvent.TakePicture -> controller.takePictureInto(event.file, context, viewModel)
            is PhotoCaptureEvent.ShowMessage -> scope.launch {
                snackbarHostState.showSnackbar(context.getString(event.messageRes))
            }

            PhotoCaptureEvent.Finished -> onClose()
        }
    }

    val measurements = uiState.measurements
    if (measurements != null) {
        // Back skips the optional step; the photos are already saved.
        BackHandler(onBack = viewModel::onSkipMeasurements)
        MeasurementsScreen(
            form = measurements,
            dismissLabel = stringResource(R.string.measurements_skip),
            actions = MeasurementsActions(
                onToggle = viewModel::onToggleMeasurement,
                onAdd = viewModel::onAddMeasurement,
                onRemove = viewModel::onRemoveMeasurement,
                onValueChange = viewModel::onMeasurementValueChange,
                onSave = viewModel::onSaveMeasurements,
                onDismiss = viewModel::onSkipMeasurements,
            ),
            snackbarHostState = snackbarHostState,
            modifier = modifier,
        )
        return
    }

    PhotoCaptureScreen(
        state = uiState,
        cameraStatus = when {
            !hasPermission -> CameraStatus.NEEDS_PERMISSION
            available != null && !available.any -> CameraStatus.UNAVAILABLE
            else -> CameraStatus.READY
        },
        canSwitchCamera = available?.front == true && available.back,
        onClose = onClose,
        onFinish = viewModel::onFinishPhotos,
        onSelectPose = viewModel::onSelectPose,
        onShutter = viewModel::onShutter,
        onCycleTimer = viewModel::onCycleTimer,
        onToggleGuidePhoto = viewModel::onToggleGuidePhoto,
        onSwitchCamera = viewModel::onSwitchCamera,
        onRetake = viewModel::onRetake,
        onUsePhoto = viewModel::onUsePhoto,
        onRequestPermission = permission.request,
        onOpenAppSettings = { context.openAppSettings() },
        snackbarHostState = snackbarHostState,
        modifier = modifier,
        viewfinder = { CameraViewfinder(controller, it) },
    )
}

/** Whether the camera may be used, and how to ask for it. */
private class CameraPermission(val granted: Boolean, val request: () -> Unit)

@Composable
private fun rememberCameraPermission(): CameraPermission {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(context.hasCameraPermission()) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasPermission = it
    }
    // Asks once on opening; after that the permission screen offers to ask again.
    var askedForPermission by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!hasPermission && !askedForPermission) {
            askedForPermission = true
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    // The user may have allowed the camera in system settings meanwhile.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { hasPermission = context.hasCameraPermission() }
    return CameraPermission(hasPermission) { permissionLauncher.launch(Manifest.permission.CAMERA) }
}

/** Binds [controller] while the camera is allowed; null until it knows which lenses there are. */
@Composable
private fun rememberLenses(controller: LifecycleCameraController, hasPermission: Boolean): Lenses? {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var lenses by remember { mutableStateOf<Lenses?>(null) }
    DisposableEffect(hasPermission, lifecycleOwner) {
        if (hasPermission) {
            controller.bindToLifecycle(lifecycleOwner)
            val initialization = controller.initializationFuture
            initialization.addListener(
                { lenses = if (runCatching { initialization.get() }.isSuccess) Lenses.of(controller) else Lenses.NONE },
                ContextCompat.getMainExecutor(context),
            )
        }
        onDispose { controller.unbind() }
    }
    return lenses
}

/** Points [controller] at the front or back camera, falling back to whichever one exists. */
@Composable
private fun SelectCamera(controller: LifecycleCameraController, available: Lenses?, useFrontCamera: Boolean) {
    LaunchedEffect(useFrontCamera, available) {
        if (available == null || !available.any) return@LaunchedEffect
        val front = if (useFrontCamera) available.front else !available.back
        val selector = if (front) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
        if (controller.cameraSelector != selector) controller.cameraSelector = selector
    }
}

/** Which cameras the device has. */
private data class Lenses(val front: Boolean, val back: Boolean) {
    val any: Boolean get() = front || back

    companion object {
        val NONE = Lenses(front = false, back = false)

        fun of(controller: LifecycleCameraController) = Lenses(
            front = controller.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA),
            back = controller.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA),
        )
    }
}

private fun createCameraController(context: Context) = LifecycleCameraController(context).apply {
    setEnabledUseCases(CameraController.IMAGE_CAPTURE)
    // Zooming would change the body's size between photos; the guides only work at one zoom.
    isPinchToZoomEnabled = false
    // Photos are portrait 3:4, so a 4:3 sensor mode loses the least to cropping.
    val fourByThree = ResolutionSelector.Builder()
        .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
        .build()
    previewResolutionSelector = fourByThree
    imageCaptureResolutionSelector = fourByThree
}

/** Saves a picture into [file]; the controller mirrors front-camera pictures to match the preview. */
private fun LifecycleCameraController.takePictureInto(file: File, context: Context, viewModel: PhotoCaptureViewModel) {
    takePicture(
        ImageCapture.OutputFileOptions.Builder(file).build(),
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) = viewModel.onPictureTaken(file)
            override fun onError(exception: ImageCaptureException) = viewModel.onPictureFailed(file)
        },
    )
}

private fun Context.hasCameraPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

private fun Context.openAppSettings() {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
    startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

/**
 * Stateless capture screen: the pose steps, a portrait frame with the camera (or the photo
 * just taken) under the pose guide, and the controls. [viewfinder] draws the live camera.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoCaptureScreen(
    state: PhotoCaptureUiState,
    cameraStatus: CameraStatus,
    canSwitchCamera: Boolean,
    onClose: () -> Unit,
    onFinish: () -> Unit,
    onSelectPose: (PhotoPose) -> Unit,
    onShutter: () -> Unit,
    onCycleTimer: () -> Unit,
    onToggleGuidePhoto: () -> Unit,
    onSwitchCamera: () -> Unit,
    onRetake: () -> Unit,
    onUsePhoto: () -> Unit,
    onRequestPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    viewfinder: @Composable (Modifier) -> Unit = {},
) {
    val poseName = stringResource(state.pose.labelRes)
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.capture_title, poseName))
                        Text(
                            text = stringResource(R.string.capture_step, state.step, PhotoPose.entries.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.photos_close))
                    }
                },
                actions = {
                    // Ends the session with poses left; after the last pose it ends by itself.
                    if (state.checkInId != null) {
                        TextButton(onClick = onFinish, enabled = state.canFinish) { Text(stringResource(R.string.capture_done)) }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        when (cameraStatus) {
            CameraStatus.NEEDS_PERMISSION -> PermissionContent(onRequestPermission, onOpenAppSettings, Modifier.padding(innerPadding))
            CameraStatus.UNAVAILABLE -> MessageContent(stringResource(R.string.capture_error_camera), Modifier.padding(innerPadding))
            CameraStatus.READY -> Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentAlignment = Alignment.TopCenter,
            ) {
                CaptureContent(
                    state = state,
                    canSwitchCamera = canSwitchCamera,
                    onSelectPose = onSelectPose,
                    onShutter = onShutter,
                    onCycleTimer = onCycleTimer,
                    onToggleGuidePhoto = onToggleGuidePhoto,
                    onSwitchCamera = onSwitchCamera,
                    onRetake = onRetake,
                    onUsePhoto = onUsePhoto,
                    viewfinder = viewfinder,
                    modifier = Modifier.widthIn(max = Dimens.maxContentWidth),
                )
            }
        }
    }
}

@Composable
private fun CaptureContent(
    state: PhotoCaptureUiState,
    canSwitchCamera: Boolean,
    onSelectPose: (PhotoPose) -> Unit,
    onShutter: () -> Unit,
    onCycleTimer: () -> Unit,
    onToggleGuidePhoto: () -> Unit,
    onSwitchCamera: () -> Unit,
    onRetake: () -> Unit,
    onUsePhoto: () -> Unit,
    viewfinder: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.spacing
    val showGuidePhoto = state.showGuidePhoto && state.guidePhotoPath != null
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = spacing.lg)
            .padding(bottom = spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PoseSelector(selected = state.pose, onSelect = onSelectPose, done = state.taken, enabled = state.canShoot)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(state.pose.hintRes),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(if (showGuidePhoto) R.string.capture_hint_align else R.string.capture_hint_general),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .aspectRatio(PHOTO_ASPECT_RATIO, matchHeightConstraintsFirst = true)
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                val review = state.reviewPath
                if (review != null) {
                    AsyncImage(
                        model = File(review),
                        contentDescription = stringResource(R.string.capture_review_description),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    viewfinder(Modifier.fillMaxSize())
                    if (showGuidePhoto) {
                        AsyncImage(
                            model = File(requireNotNull(state.guidePhotoPath)),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(GUIDE_PHOTO_ALPHA),
                        )
                    }
                    PoseGuide(pose = state.pose, modifier = Modifier.fillMaxSize())
                    state.countdown?.let { CountdownBadge(it) }
                }
            }
        }
        if (state.reviewPath != null) {
            ReviewControls(isSaving = state.isSaving, onRetake = onRetake, onUse = onUsePhoto)
        } else {
            Text(
                text = stringResource(R.string.capture_tip),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            CameraControls(
                state = state,
                canSwitchCamera = canSwitchCamera,
                onShutter = onShutter,
                onCycleTimer = onCycleTimer,
                onToggleGuidePhoto = onToggleGuidePhoto,
                onSwitchCamera = onSwitchCamera,
            )
        }
    }
}

@Composable
private fun PermissionContent(
    onRequestPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(MaterialTheme.spacing.xl),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.capture_permission_title), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(
            text = stringResource(R.string.capture_permission_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        FilledTonalButton(onClick = onRequestPermission) { Text(stringResource(R.string.capture_permission_allow)) }
        // Once the user has denied twice, Android stops asking; only settings can allow it then.
        TextButton(onClick = onOpenAppSettings) { Text(stringResource(R.string.capture_permission_settings)) }
    }
}
