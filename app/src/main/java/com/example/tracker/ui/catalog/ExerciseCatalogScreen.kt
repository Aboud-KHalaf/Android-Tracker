package com.example.tracker.ui.catalog

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tracker.R
import com.example.tracker.domain.model.ExerciseType
import com.example.tracker.ui.catalog.components.CatalogExerciseRow
import com.example.tracker.ui.catalog.components.CatalogExerciseSheet
import com.example.tracker.ui.common.EmptyState
import com.example.tracker.ui.common.ErrorContent
import com.example.tracker.ui.common.LoadingContent
import com.example.tracker.ui.common.ObserveAsEvents
import com.example.tracker.ui.common.StateCrossfade
import com.example.tracker.ui.theme.Dimens
import com.example.tracker.ui.theme.spacing
import kotlinx.coroutines.launch

/**
 * Exercise catalog destination: connects [ExerciseCatalogViewModel] to [ExerciseCatalogScreen]
 * and turns its events into snackbars.
 */
@Composable
fun ExerciseCatalogRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExerciseCatalogViewModel = viewModel(factory = ExerciseCatalogViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    ObserveAsEvents(viewModel.events) { event ->
        val message = when (event) {
            is ExerciseCatalogEvent.ExerciseAdded -> context.getString(R.string.catalog_added, event.exerciseName)
            is ExerciseCatalogEvent.ShowMessage -> context.getString(event.messageRes)
        }
        // Launched so a newer message replaces this one instead of waiting for it.
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    ExerciseCatalogScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        actions = ExerciseCatalogActions(
            onBack = onBack,
            onQueryChange = viewModel::onQueryChange,
            onRetry = viewModel::onRetry,
            onSelect = viewModel::onSelect,
            onDismissDetails = viewModel::onDismissDetails,
            onAdd = viewModel::onAddSelected,
        ),
        modifier = modifier,
    )
}

/** User actions on the Exercise catalog screen. */
data class ExerciseCatalogActions(
    val onBack: () -> Unit = {},
    val onQueryChange: (String) -> Unit = {},
    val onRetry: () -> Unit = {},
    val onSelect: (exerciseId: Int) -> Unit = {},
    val onDismissDetails: () -> Unit = {},
    val onAdd: (ExerciseType) -> Unit = {},
)

/** Stateless Exercise catalog screen: a search field over the matching exercises. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseCatalogScreen(
    uiState: ExerciseCatalogUiState,
    actions: ExerciseCatalogActions,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.catalog_title)) },
                navigationIcon = {
                    IconButton(onClick = actions.onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(top = innerPadding.calculateTopPadding())
                .fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(modifier = Modifier.widthIn(max = Dimens.maxContentWidth)) {
                SearchField(
                    query = uiState.query,
                    onQueryChange = actions.onQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MaterialTheme.spacing.lg, vertical = MaterialTheme.spacing.sm),
                )
                StateCrossfade(uiState.results) { results ->
                    when (results) {
                        CatalogResultsUi.Loading -> LoadingContent()
                        CatalogResultsUi.Error -> ErrorContent(
                            message = stringResource(R.string.catalog_error_load),
                            onRetry = actions.onRetry,
                        )

                        is CatalogResultsUi.Success -> CatalogResults(
                            exercises = results.exercises,
                            onSelect = actions.onSelect,
                            bottomPadding = innerPadding.calculateBottomPadding(),
                        )
                    }
                }
            }
        }
    }

    uiState.selected?.let { exercise ->
        CatalogExerciseSheet(
            exercise = exercise,
            isAdding = uiState.isAdding,
            onAdd = actions.onAdd,
            onDismiss = actions.onDismissDetails,
        )
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(stringResource(R.string.catalog_search_label)) },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.catalog_search_clear))
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        shape = MaterialTheme.shapes.extraLarge,
        modifier = modifier,
    )
}

@Composable
private fun CatalogResults(
    exercises: List<CatalogExerciseUi>,
    onSelect: (Int) -> Unit,
    bottomPadding: Dp,
) {
    if (exercises.isEmpty()) {
        EmptyState(
            icon = Icons.Outlined.SearchOff,
            title = stringResource(R.string.catalog_empty_title),
            body = stringResource(R.string.catalog_empty_body),
        )
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = bottomPadding + MaterialTheme.spacing.lg),
    ) {
        items(exercises, key = { it.id }) { exercise ->
            CatalogExerciseRow(exercise = exercise, onClick = { onSelect(exercise.id) })
        }
        item(key = "attribution") {
            Text(
                text = stringResource(R.string.catalog_attribution),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(MaterialTheme.spacing.lg),
            )
        }
    }
}
