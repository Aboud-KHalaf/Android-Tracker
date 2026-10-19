package com.example.tracker.ui.photos

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
import com.example.tracker.domain.photos.comparisonOf
import com.example.tracker.domain.photos.measurementChanges
import com.example.tracker.domain.repository.ProgressPhotoRepository
import com.example.tracker.ui.common.launchCatching
import com.example.tracker.ui.photos.measurements.MeasurementsForm
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * Every photo check-in with its measurements, newest first, a first-versus-latest comparison
 * for one pose, and editing a check-in's measurements.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProgressPhotosViewModel(
    private val photoRepository: ProgressPhotoRepository,
) : ViewModel() {

    private val loadAttempt = MutableStateFlow(0)
    private val comparePose = MutableStateFlow(PhotoPose.FRONT)

    val uiState: StateFlow<ProgressPhotosUiState> = loadAttempt
        .flatMapLatest {
            combine(photoRepository.observeCheckIns(), comparePose) { checkIns, pose ->
                ProgressPhotosUiState.Success(checkIns.toUi(), pose, comparisonOf(checkIns, pose)) as ProgressPhotosUiState
            }
                .onStart { emit(ProgressPhotosUiState.Loading) }
                .catch { emit(ProgressPhotosUiState.Error) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ProgressPhotosUiState.Loading)

    private val _measurements = MutableStateFlow<MeasurementsForm?>(null)

    /** The check-in whose measurements are being edited, or null. */
    val measurements: StateFlow<MeasurementsForm?> = _measurements.asStateFlow()

    private val _events = Channel<ProgressPhotosEvent>(Channel.BUFFERED)
    val events: Flow<ProgressPhotosEvent> = _events.receiveAsFlow()

    fun onSelectComparePose(pose: PhotoPose) {
        comparePose.value = pose
    }

    fun onDeletePhoto(id: String) {
        viewModelScope.launchCatching(onError = { showMessage(R.string.photos_error_delete) }) {
            photoRepository.deletePhoto(id)
            showMessage(R.string.photos_deleted)
        }
    }

    /** Deletes a whole check-in; the screen has already asked to confirm. */
    fun onDeleteCheckIn(id: String) {
        viewModelScope.launchCatching(onError = { showMessage(R.string.photos_error_delete) }) {
            photoRepository.deleteCheckIn(id)
            showMessage(R.string.photos_check_in_deleted)
        }
    }

    fun onEditMeasurements(checkIn: PhotoCheckIn) {
        _measurements.value = MeasurementsForm.editing(checkIn.id, checkIn.measurements)
    }

    fun onToggleMeasurement(name: String, unit: String) = _measurements.update { it?.toggled(name, unit) }

    fun onAddMeasurement(name: String, unit: String) = _measurements.update { it?.added(name, unit) }

    fun onRemoveMeasurement(index: Int) = _measurements.update { it?.removed(index) }

    fun onMeasurementValueChange(index: Int, text: String) = _measurements.update { it?.withValue(index, text) }

    fun onDismissMeasurements() {
        _measurements.value = null
    }

    fun onSaveMeasurements() {
        val form = _measurements.value ?: return
        if (form.isSaving) return
        val (checked, measurements) = form.validated()
        if (measurements == null) {
            _measurements.value = checked
            return
        }
        _measurements.value = form.copy(isSaving = true)
        viewModelScope.launchCatching(
            onError = {
                _measurements.update { it?.copy(isSaving = false) }
                showMessage(R.string.measurements_error_save)
            },
        ) {
            photoRepository.saveMeasurements(form.checkInId, measurements)
            _measurements.value = null
        }
    }

    fun onRetry() {
        loadAttempt.update { it + 1 }
    }

    private suspend fun showMessage(@StringRes messageRes: Int) {
        _events.send(ProgressPhotosEvent.ShowMessage(messageRes))
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as TrackerApplication).container
                ProgressPhotosViewModel(container.progressPhotoRepository)
            }
        }
    }
}

private fun List<PhotoCheckIn>.toUi(): List<CheckInUi> {
    val perDay = groupingBy { it.date }.eachCount()
    return map { checkIn ->
        CheckInUi(
            checkIn = checkIn,
            changes = measurementChanges(this, checkIn),
            sharesDay = (perDay[checkIn.date] ?: 0) > 1,
        )
    }
}
