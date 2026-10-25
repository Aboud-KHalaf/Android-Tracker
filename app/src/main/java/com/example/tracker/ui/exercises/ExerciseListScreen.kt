package com.example.tracker.ui.exercises

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tracker.R
import com.example.tracker.domain.model.MuscleGroup
import com.example.tracker.ui.common.EmptyState
import com.example.tracker.ui.common.ErrorContent
import com.example.tracker.ui.common.IconAvatar
import com.example.tracker.ui.common.LoadingContent
import com.example.tracker.ui.common.StateCrossfade
import com.example.tracker.ui.common.icon
import com.example.tracker.ui.common.label
import com.example.tracker.ui.exercises.components.MuscleWeekCard
import com.example.tracker.ui.theme.Dimens
import com.example.tracker.ui.theme.spacing

/** Progress tab destination: connects [ExerciseListViewModel] to [ExerciseListScreen]. */
@Composable
fun ExerciseListRoute(
    onOpenExercise: (exerciseId: String) -> Unit,
    onOpenHistory: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExerciseListViewModel = viewModel(factory = ExerciseListViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    ExerciseListScreen(
        uiState,
        query = query,
        actions = ExerciseListActions(
            onOpenExercise = onOpenExercise,
            onOpenHistory = onOpenHistory,
            onRetry = viewModel::onRetry,
            onQueryChange = viewModel::onQueryChange,
            onMuscleFilterChange = viewModel::onMuscleFilterChange,
            onClearFilters = viewModel::onClearFilters,
        ),
        modifier = modifier,
    )
}

/** What the user can do on the Progress tab. */
data class ExerciseListActions(
    val onOpenExercise: (exerciseId: String) -> Unit = {},
    val onOpenHistory: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onQueryChange: (String) -> Unit = {},
    /** Null shows every exercise. */
    val onMuscleFilterChange: (MuscleGroup?) -> Unit = {},
    val onClearFilters: () -> Unit = {},
)

/**
 * Stateless Progress tab: every exercise, each opening its progress, searchable by name and
 * filterable by muscle group. The large title collapses into the top bar as the list scrolls.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseListScreen(
    uiState: ExerciseListUiState,
    query: String,
    actions: ExerciseListActions,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.exercises_title)) },
                actions = {
                    IconButton(onClick = actions.onOpenHistory) {
                        Icon(Icons.Outlined.History, contentDescription = stringResource(R.string.history_open))
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        StateCrossfade(uiState) { state ->
            when (state) {
                ExerciseListUiState.Loading -> LoadingContent(Modifier.padding(innerPadding))
                ExerciseListUiState.Error -> ErrorContent(
                    message = stringResource(R.string.exercises_error_load),
                    onRetry = actions.onRetry,
                    modifier = Modifier.padding(innerPadding),
                )

                is ExerciseListUiState.Success -> ExerciseList(state, query, actions, innerPadding)
            }
        }
    }
}

@Composable
private fun ExerciseList(
    state: ExerciseListUiState.Success,
    query: String,
    actions: ExerciseListActions,
    contentPadding: PaddingValues,
) {
    val spacing = MaterialTheme.spacing
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = Dimens.maxContentWidth)
                .fillMaxSize(),
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding(),
                bottom = contentPadding.calculateBottomPadding() + spacing.lg,
            ),
        ) {
            if (state.muscleWeek.isNotEmpty()) {
                item(key = "muscle-week") {
                    MuscleWeekCard(
                        rows = state.muscleWeek,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = spacing.lg, end = spacing.lg, bottom = spacing.md),
                    )
                }
            }
            if (!state.hasExercises) {
                item(key = "empty") {
                    EmptyState(
                        icon = Icons.Outlined.Insights,
                        title = stringResource(R.string.exercises_empty_title),
                        body = stringResource(R.string.exercises_empty_body),
                    )
                }
                return@LazyColumn
            }
            // Stays pinned below the top bar, so the search is at hand anywhere in a long list.
            stickyHeader(key = "search") {
                SearchAndFilter(
                    query = query,
                    muscleFilter = state.muscleFilter,
                    muscleOptions = state.muscleOptions,
                    actions = actions,
                )
            }
            if (state.exercises.isEmpty()) {
                item(key = "no-matches") { NoMatches(onClear = actions.onClearFilters) }
            }
            items(state.exercises, key = { it.id }) { exercise ->
                ExerciseRow(exercise, onClick = { actions.onOpenExercise(exercise.id) }, modifier = Modifier.animateItem())
            }
        }
    }
}

/** The search field with a row of muscle group chips below it, on the screen's background. */
@Composable
private fun SearchAndFilter(
    query: String,
    muscleFilter: MuscleGroup?,
    muscleOptions: List<MuscleGroup>,
    actions: ExerciseListActions,
) {
    val spacing = MaterialTheme.spacing
    val focusManager = LocalFocusManager.current
    Surface(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = spacing.sm)) {
            OutlinedTextField(
                value = query,
                onValueChange = actions.onQueryChange,
                placeholder = { Text(stringResource(R.string.exercises_search)) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                trailingIcon = if (query.isNotEmpty()) {
                    {
                        IconButton(onClick = { actions.onQueryChange("") }) {
                            Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.exercises_search_clear))
                        }
                    }
                } else {
                    null
                },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.lg),
            )
            if (muscleOptions.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = spacing.lg),
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                    modifier = Modifier.padding(top = spacing.sm),
                ) {
                    item(key = "all") {
                        FilterChip(
                            selected = muscleFilter == null,
                            onClick = { actions.onMuscleFilterChange(null) },
                            label = { Text(stringResource(R.string.exercises_filter_all)) },
                        )
                    }
                    items(muscleOptions, key = { it.name }) { group ->
                        val selected = group == muscleFilter
                        FilterChip(
                            selected = selected,
                            // Tapping the chosen group again shows every exercise.
                            onClick = { actions.onMuscleFilterChange(if (selected) null else group) },
                            label = { Text(group.label()) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseRow(exercise: ExerciseItemUi, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val separator = stringResource(R.string.separator_dot)
    ListItem(
        headlineContent = { Text(exercise.name) },
        supportingContent = {
            Text(listOfNotNull(exercise.type.label(), exercise.muscleGroup?.label()).joinToString(separator))
        },
        leadingContent = {
            IconAvatar(
                icon = exercise.type.icon,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        },
        trailingContent = { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null) },
        modifier = modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun NoMatches(onClear: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        EmptyState(
            icon = Icons.Outlined.SearchOff,
            title = stringResource(R.string.exercises_no_matches_title),
            body = stringResource(R.string.exercises_no_matches_body),
        )
        TextButton(onClick = onClear) { Text(stringResource(R.string.exercises_clear_filters)) }
    }
}
