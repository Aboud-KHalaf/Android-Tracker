package com.example.tracker.ui.photos

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tracker.R
import com.example.tracker.domain.model.PhotoPose
import com.example.tracker.domain.model.ProgressPhoto
import com.example.tracker.domain.photos.PhotoCheckIn
import com.example.tracker.ui.common.AnimatedFab
import com.example.tracker.ui.common.DestructiveConfirmDialog
import com.example.tracker.ui.common.EmptyState
import com.example.tracker.ui.common.ErrorContent
import com.example.tracker.ui.common.LoadingContent
import com.example.tracker.ui.common.ObserveAsEvents
import com.example.tracker.ui.common.SectionHeader
import com.example.tracker.ui.common.StateCrossfade
import com.example.tracker.ui.photos.components.CheckInRow
import com.example.tracker.ui.photos.components.ComparisonCard
import com.example.tracker.ui.photos.components.PhotoViewerDialog
import com.example.tracker.ui.photos.measurements.MeasurementsActions
import com.example.tracker.ui.photos.measurements.MeasurementsScreen
import com.example.tracker.ui.theme.Dimens
import com.example.tracker.ui.theme.spacing
import kotlinx.coroutines.launch

/** Space below the list so the last row can scroll clear of the floating button. */
private val FabClearance = 88.dp

/** Progress photos destination: connects [ProgressPhotosViewModel] to [ProgressPhotosScreen]. */
@Composable
fun ProgressPhotosRoute(
    onBack: () -> Unit,
    onTakePhotos: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProgressPhotosViewModel = viewModel(factory = ProgressPhotosViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val measurements by viewModel.measurements.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is ProgressPhotosEvent.ShowMessage -> scope.launch {
                snackbarHostState.showSnackbar(context.getString(event.messageRes))
            }
        }
    }

    ProgressPhotosScreen(
        uiState = uiState,
        onBack = onBack,
        onTakePhotos = onTakePhotos,
        onSelectComparePose = viewModel::onSelectComparePose,
        onDeletePhoto = viewModel::onDeletePhoto,
        onEditMeasurements = viewModel::onEditMeasurements,
        onDeleteCheckIn = viewModel::onDeleteCheckIn,
        onRetry = viewModel::onRetry,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )

    measurements?.let { form ->
        Dialog(
            onDismissRequest = viewModel::onDismissMeasurements,
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
        ) {
            MeasurementsScreen(
                form = form,
                dismissLabel = stringResource(R.string.action_cancel),
                actions = MeasurementsActions(
                    onToggle = viewModel::onToggleMeasurement,
                    onAdd = viewModel::onAddMeasurement,
                    onRemove = viewModel::onRemoveMeasurement,
                    onValueChange = viewModel::onMeasurementValueChange,
                    onSave = viewModel::onSaveMeasurements,
                    onDismiss = viewModel::onDismissMeasurements,
                ),
            )
        }
    }
}

/** Stateless Progress photos screen: a comparison, then every check-in, newest first. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressPhotosScreen(
    uiState: ProgressPhotosUiState,
    onBack: () -> Unit,
    onTakePhotos: () -> Unit,
    onSelectComparePose: (PhotoPose) -> Unit,
    onDeletePhoto: (String) -> Unit,
    onEditMeasurements: (PhotoCheckIn) -> Unit,
    onDeleteCheckIn: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    var openPhotoId by rememberSaveable { mutableStateOf<String?>(null) }
    var deletingCheckInId by rememberSaveable { mutableStateOf<String?>(null) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.photos_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            AnimatedFab(visible = uiState is ProgressPhotosUiState.Success) {
                ExtendedFloatingActionButton(
                    text = { Text(stringResource(R.string.photos_take)) },
                    icon = { Icon(Icons.Outlined.PhotoCamera, contentDescription = null) },
                    onClick = onTakePhotos,
                )
            }
        },
    ) { innerPadding ->
        StateCrossfade(uiState) { state ->
            when (state) {
                ProgressPhotosUiState.Loading -> LoadingContent(Modifier.padding(innerPadding))
                ProgressPhotosUiState.Error -> ErrorContent(
                    message = stringResource(R.string.photos_error_load),
                    onRetry = onRetry,
                    modifier = Modifier.padding(innerPadding),
                )

                is ProgressPhotosUiState.Success -> PhotosContent(
                    state = state,
                    onSelectComparePose = onSelectComparePose,
                    onOpenPhoto = { openPhotoId = it.id },
                    onEditMeasurements = onEditMeasurements,
                    onDeleteCheckIn = { deletingCheckInId = it },
                    contentPadding = innerPadding,
                )
            }
        }
    }

    val openPhoto = (uiState as? ProgressPhotosUiState.Success)?.findPhoto(openPhotoId)
    if (openPhoto != null) {
        PhotoViewerDialog(
            photo = openPhoto,
            onDelete = {
                openPhotoId = null
                onDeletePhoto(openPhoto.id)
            },
            onDismiss = { openPhotoId = null },
        )
    }

    deletingCheckInId?.let { id ->
        DestructiveConfirmDialog(
            title = stringResource(R.string.photos_check_in_delete_title),
            body = stringResource(R.string.photos_check_in_delete_body),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = {
                deletingCheckInId = null
                onDeleteCheckIn(id)
            },
            onDismiss = { deletingCheckInId = null },
        )
    }
}

private fun ProgressPhotosUiState.Success.findPhoto(id: String?): ProgressPhoto? =
    id?.let { checkIns.firstNotNullOfOrNull { item -> item.checkIn.photos.values.firstOrNull { it.id == id } } }

@Composable
private fun PhotosContent(
    state: ProgressPhotosUiState.Success,
    onSelectComparePose: (PhotoPose) -> Unit,
    onOpenPhoto: (ProgressPhoto) -> Unit,
    onEditMeasurements: (PhotoCheckIn) -> Unit,
    onDeleteCheckIn: (String) -> Unit,
    contentPadding: PaddingValues,
) {
    val spacing = MaterialTheme.spacing
    val rowModifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = spacing.lg, vertical = spacing.sm)
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = Dimens.maxContentWidth)
                .fillMaxSize(),
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding(),
                bottom = contentPadding.calculateBottomPadding() + FabClearance,
            ),
        ) {
            if (state.checkIns.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        icon = Icons.Outlined.PhotoLibrary,
                        title = stringResource(R.string.photos_empty_title),
                        body = stringResource(R.string.photos_empty_body),
                    )
                }
                return@LazyColumn
            }
            item(key = "compare") {
                ComparisonCard(
                    pose = state.comparePose,
                    comparison = state.comparison,
                    onSelectPose = onSelectComparePose,
                    onOpenPhoto = onOpenPhoto,
                    modifier = rowModifier,
                )
            }
            item(key = "history") {
                SectionHeader(
                    title = stringResource(R.string.photos_history),
                    modifier = Modifier.padding(start = spacing.lg, end = spacing.lg, top = spacing.md),
                )
            }
            items(state.checkIns, key = { it.checkIn.id }) { item ->
                CheckInRow(
                    item = item,
                    onOpenPhoto = onOpenPhoto,
                    onEditMeasurements = { onEditMeasurements(item.checkIn) },
                    onDelete = { onDeleteCheckIn(item.checkIn.id) },
                    modifier = rowModifier.animateItem(),
                )
            }
        }
    }
}
