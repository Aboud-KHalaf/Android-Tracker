package com.example.tracker.ui.photos.capture

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.tracker.R
import com.example.tracker.TrackerApplication
import com.example.tracker.domain.model.PhotoPose
import com.example.tracker.domain.photos.PhotoCheckIn
import com.example.tracker.domain.photos.guidePhotoFor
import com.example.tracker.domain.repository.ProgressPhotoRepository
import com.example.tracker.ui.common.launchCatching
import com.example.tracker.ui.photos.measurements.MeasurementsForm
import java.io.File
import java.io.InputStream
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Walks through a new check-in's front, side and back photos, then offers to note
 * measurements with them. Each pose shows the last photo of it to line up with; a self-timer
 * gives time to step back. The screen owns the camera: it takes a picture on
 * [PhotoCaptureEvent.TakePicture] and reports the result.
 */
class PhotoCaptureViewModel(
    private val photoRepository: ProgressPhotoRepository,
) : ViewModel() {

    private val controls = MutableStateFlow(PhotoCaptureUiState())

    // Without the saved photos the camera still works; it just has nothing to line up with.
    private val checkIns = photoRepository.observeCheckIns().catch { emit(emptyList()) }

    val uiState: StateFlow<PhotoCaptureUiState> = combine(controls, checkIns) { state, checkIns ->
        state.withCheckIns(checkIns)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), PhotoCaptureUiState())

    private val _events = Channel<PhotoCaptureEvent>(Channel.BUFFERED)
    val events: Flow<PhotoCaptureEvent> = _events.receiveAsFlow()

    private var countdownJob: Job? = null
    private var pendingCapture: File? = null

    fun onSelectPose(pose: PhotoPose) {
        if (!controls.value.canShoot) return
        stopCountdown()
        controls.update { it.copy(pose = pose) }
    }

    /** Picks the self-timer length; it can't change while it counts down. */
    fun onSelectTimer(timer: CaptureTimer) {
        if (controls.value.countdown != null) return
        controls.update { it.copy(timer = timer) }
    }

    fun onSwitchCamera() {
        controls.update { it.copy(useFrontCamera = !it.useFrontCamera) }
    }

    fun onToggleGuidePhoto() {
        controls.update { it.copy(showGuidePhoto = !it.showGuidePhoto) }
    }

    /** Starts the self-timer, or takes the photo straight away; while the timer runs, stops it. */
    fun onShutter() {
        val state = controls.value
        if (state.countdown != null) {
            stopCountdown()
            return
        }
        if (!state.canShoot) return
        countdownJob = viewModelScope.launchCatching(onError = { onTakePictureFailed() }) {
            for (secondsLeft in state.timer.seconds downTo 1) {
                controls.update { it.copy(countdown = secondsLeft) }
                delay(ONE_SECOND_MILLIS)
            }
            controls.update { it.copy(countdown = null) }
            takePicture()
        }
    }

    private suspend fun takePicture() {
        controls.update { it.copy(isTakingPicture = true) }
        val file = photoRepository.createCaptureFile()
        _events.send(PhotoCaptureEvent.TakePicture(file))
    }

    private suspend fun onTakePictureFailed() {
        controls.update { it.copy(isTakingPicture = false) }
        showMessage(R.string.capture_error_take)
    }

    /**
     * The user picked a picture from the gallery, which [open] reads: it is copied in and shown
     * for review, just like one from the camera.
     */
    fun onPickFromGallery(open: () -> InputStream) {
        if (!controls.value.canShoot) return
        stopCountdown()
        controls.update { it.copy(isTakingPicture = true) }
        viewModelScope.launchCatching(
            onError = {
                controls.update { it.copy(isTakingPicture = false) }
                showMessage(R.string.capture_error_import)
            },
        ) { onPictureTaken(photoRepository.importCapture(open)) }
    }

    /** The camera wrote the picture into [file]; show it for review. */
    fun onPictureTaken(file: File) {
        pendingCapture = file
        controls.update { it.copy(isTakingPicture = false, reviewPath = file.absolutePath) }
    }

    fun onPictureFailed(file: File) {
        controls.update { it.copy(isTakingPicture = false) }
        viewModelScope.launchCatching(onError = {}) { photoRepository.discardCapture(file) }
        viewModelScope.launch { showMessage(R.string.capture_error_take) }
    }

    fun onRetake() {
        val capture = pendingCapture ?: return
        if (controls.value.isSaving) return
        pendingCapture = null
        controls.update { it.copy(reviewPath = null) }
        viewModelScope.launchCatching(onError = {}) { photoRepository.discardCapture(capture) }
    }

    /**
     * Keeps the reviewed picture as this session's photo of the pose and moves on to the next
     * pose not taken yet, or to the measurements once all are.
     */
    fun onUsePhoto() {
        val capture = pendingCapture ?: return
        val state = uiState.value
        if (state.isSaving) return
        controls.update { it.copy(isSaving = true) }
        viewModelScope.launchCatching(
            onError = {
                controls.update { it.copy(isSaving = false) }
                showMessage(R.string.capture_error_save)
            },
        ) {
            val checkInId = photoRepository.savePhoto(state.checkInId, state.pose, capture)
            pendingCapture = null
            val next = nextPose(state.pose, state.taken + state.pose)
            controls.update { it.copy(checkInId = checkInId, pose = next ?: it.pose, reviewPath = null, isSaving = false) }
            if (next == null) onFinishPhotos()
        }
    }

    /** Ends the photos, even with poses left, and asks for measurements, starting with last time's. */
    fun onFinishPhotos() {
        // The controls, not uiState, which may not have caught up with a photo just saved.
        val controls = controls.value
        val checkInId = controls.checkInId ?: return
        if (!controls.canFinish || controls.measurements != null) return
        stopCountdown()
        val previous = uiState.value.previousMeasurements
        this.controls.update { it.copy(measurements = MeasurementsForm.newFor(checkInId, previous)) }
    }

    fun onToggleMeasurement(name: String, unit: String) = updateMeasurements { it.toggled(name, unit) }

    fun onAddMeasurement(name: String, unit: String) = updateMeasurements { it.added(name, unit) }

    fun onRemoveMeasurement(index: Int) = updateMeasurements { it.removed(index) }

    fun onMeasurementValueChange(index: Int, text: String) = updateMeasurements { it.withValue(index, text) }

    fun onSkipMeasurements() {
        if (controls.value.measurements?.isSaving == true) return
        viewModelScope.launch { _events.send(PhotoCaptureEvent.Finished) }
    }

    fun onSaveMeasurements() {
        val form = controls.value.measurements ?: return
        if (form.isSaving) return
        val (checked, measurements) = form.validated()
        if (measurements == null) {
            updateMeasurements { checked }
            return
        }
        updateMeasurements { it.copy(isSaving = true) }
        viewModelScope.launchCatching(
            onError = {
                updateMeasurements { it.copy(isSaving = false) }
                showMessage(R.string.measurements_error_save)
            },
        ) {
            photoRepository.saveMeasurements(form.checkInId, measurements)
            _events.send(PhotoCaptureEvent.Finished)
        }
    }

    private inline fun updateMeasurements(crossinline transform: (MeasurementsForm) -> MeasurementsForm) {
        controls.update { state -> state.measurements?.let { state.copy(measurements = transform(it)) } ?: state }
    }

    private fun stopCountdown() {
        countdownJob?.cancel()
        countdownJob = null
        controls.update { it.copy(countdown = null) }
    }

    private fun PhotoCaptureUiState.withCheckIns(checkIns: List<PhotoCheckIn>): PhotoCaptureUiState = copy(
        taken = checkIns.firstOrNull { it.id == checkInId }?.photos?.keys.orEmpty(),
        guidePhotoPath = guidePhotoFor(checkIns, pose, checkInId)?.filePath,
        previousMeasurements = checkIns.firstOrNull { it.id != checkInId && it.measurements.isNotEmpty() }?.measurements.orEmpty(),
    )

    private suspend fun showMessage(@StringRes messageRes: Int) {
        _events.send(PhotoCaptureEvent.ShowMessage(messageRes))
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L
        private const val ONE_SECOND_MILLIS = 1_000L

        /** The first pose after [current], in order and wrapping around, that isn't in [taken]. */
        internal fun nextPose(current: PhotoPose, taken: Set<PhotoPose>): PhotoPose? {
            val poses = PhotoPose.entries
            return (1 until poses.size)
                .map { poses[(current.ordinal + it) % poses.size] }
                .firstOrNull { it !in taken }
        }

        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as TrackerApplication).container
                PhotoCaptureViewModel(container.progressPhotoRepository)
            }
        }
    }
}
