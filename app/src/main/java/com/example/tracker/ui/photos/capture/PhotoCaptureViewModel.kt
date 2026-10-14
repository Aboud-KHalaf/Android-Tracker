package com.example.tracker.ui.photos.capture

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.tracker.R
import com.example.tracker.TrackerApplication
import com.example.tracker.core.TimeProvider
import com.example.tracker.domain.model.PhotoPose
import com.example.tracker.domain.model.ProgressPhoto
import com.example.tracker.domain.photos.guidePhotoFor
import com.example.tracker.domain.repository.ProgressPhotoRepository
import com.example.tracker.ui.common.launchCatching
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
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
 * Walks through today's front, side and back photos. Each pose shows the last photo of it to
 * line up with; a self-timer gives time to step back. The screen owns the camera: it takes a
 * picture on [PhotoCaptureEvent.TakePicture] and reports the result.
 */
class PhotoCaptureViewModel(
    private val photoRepository: ProgressPhotoRepository,
    private val time: TimeProvider,
) : ViewModel() {

    private val controls = MutableStateFlow(PhotoCaptureUiState())

    // Without the saved photos the camera still works; it just has nothing to line up with.
    private val photos = photoRepository.observePhotos().catch { emit(emptyList()) }

    val uiState: StateFlow<PhotoCaptureUiState> = combine(controls, photos) { state, photos ->
        state.withPhotos(photos)
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

    fun onCycleTimer() {
        controls.update { it.copy(timer = it.timer.next()) }
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
        countdownJob = viewModelScope.launch {
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
        try {
            val file = photoRepository.createCaptureFile()
            _events.send(PhotoCaptureEvent.TakePicture(file))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            controls.update { it.copy(isTakingPicture = false) }
            showMessage(R.string.capture_error_take)
        }
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

    /** Keeps the reviewed picture as today's photo of the pose and moves on to the next pose not taken yet. */
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
            photoRepository.savePhoto(today(), state.pose, capture)
            pendingCapture = null
            val next = nextPose(state.pose, state.takenToday + state.pose)
            controls.update { it.copy(pose = next ?: it.pose, reviewPath = null, isSaving = false) }
            if (next == null) _events.send(PhotoCaptureEvent.Finished)
        }
    }

    private fun stopCountdown() {
        countdownJob?.cancel()
        countdownJob = null
        controls.update { it.copy(countdown = null) }
    }

    private fun PhotoCaptureUiState.withPhotos(photos: List<ProgressPhoto>): PhotoCaptureUiState {
        val today = today()
        return copy(
            takenToday = photos.filter { it.date == today }.mapTo(mutableSetOf()) { it.pose },
            guidePhotoPath = guidePhotoFor(photos, pose, today)?.filePath,
        )
    }

    private fun today(): LocalDate = time.now().atZone(time.zone()).toLocalDate()

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
                PhotoCaptureViewModel(container.progressPhotoRepository, container.time)
            }
        }
    }
}
