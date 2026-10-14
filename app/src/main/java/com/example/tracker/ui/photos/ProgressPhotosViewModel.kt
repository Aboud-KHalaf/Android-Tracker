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
import com.example.tracker.domain.photos.checkInsOf
import com.example.tracker.domain.photos.comparisonOf
import com.example.tracker.domain.repository.ProgressPhotoRepository
import com.example.tracker.ui.common.launchCatching
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/** Every progress photo by day, with a first-versus-latest comparison for one pose. */
@OptIn(ExperimentalCoroutinesApi::class)
class ProgressPhotosViewModel(
    private val photoRepository: ProgressPhotoRepository,
) : ViewModel() {

    private val loadAttempt = MutableStateFlow(0)
    private val comparePose = MutableStateFlow(PhotoPose.FRONT)

    val uiState: StateFlow<ProgressPhotosUiState> = loadAttempt
        .flatMapLatest {
            combine(photoRepository.observePhotos(), comparePose) { photos, pose ->
                ProgressPhotosUiState.Success(checkInsOf(photos), pose, comparisonOf(photos, pose)) as ProgressPhotosUiState
            }
                .onStart { emit(ProgressPhotosUiState.Loading) }
                .catch { emit(ProgressPhotosUiState.Error) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ProgressPhotosUiState.Loading)

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
