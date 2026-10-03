package com.example.tracker.ui.exercises

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tracker.R
import com.example.tracker.ui.common.EmptyState
import com.example.tracker.ui.common.ErrorContent
import com.example.tracker.ui.common.IconAvatar
import com.example.tracker.ui.common.LoadingContent
import com.example.tracker.ui.common.ScreenTitle
import com.example.tracker.ui.common.StateCrossfade
import com.example.tracker.ui.common.icon
import com.example.tracker.ui.common.label
import com.example.tracker.ui.theme.Dimens
import com.example.tracker.ui.theme.spacing

/** Progress tab destination: connects [ExerciseListViewModel] to [ExerciseListScreen]. */
@Composable
fun ExerciseListRoute(
    onOpenExercise: (exerciseId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExerciseListViewModel = viewModel(factory = ExerciseListViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ExerciseListScreen(uiState, onOpenExercise = onOpenExercise, onRetry = viewModel::onRetry, modifier = modifier)
}

/** Stateless Progress tab: every exercise, each opening its progress. */
@Composable
fun ExerciseListScreen(
    uiState: ExerciseListUiState,
    onOpenExercise: (exerciseId: String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier) { innerPadding ->
        StateCrossfade(uiState) { state ->
            when (state) {
                ExerciseListUiState.Loading -> LoadingContent(Modifier.padding(innerPadding))
                ExerciseListUiState.Error -> ErrorContent(
                    message = stringResource(R.string.exercises_error_load),
                    onRetry = onRetry,
                    modifier = Modifier.padding(innerPadding),
                )

                is ExerciseListUiState.Success -> ExerciseList(state.exercises, onOpenExercise, innerPadding)
            }
        }
    }
}

@Composable
private fun ExerciseList(
    exercises: List<ExerciseItemUi>,
    onOpenExercise: (String) -> Unit,
    contentPadding: PaddingValues,
) {
    val spacing = MaterialTheme.spacing
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = Dimens.maxContentWidth)
                .fillMaxSize(),
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding() + spacing.xl,
                bottom = contentPadding.calculateBottomPadding() + spacing.lg,
            ),
        ) {
            item(key = "title") {
                ScreenTitle(
                    text = stringResource(R.string.exercises_title),
                    modifier = Modifier.padding(start = spacing.lg, end = spacing.lg, bottom = spacing.sm),
                )
            }
            if (exercises.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        icon = Icons.Outlined.Insights,
                        title = stringResource(R.string.exercises_empty_title),
                        body = stringResource(R.string.exercises_empty_body),
                    )
                }
            }
            items(exercises, key = { it.id }) { exercise ->
                ListItem(
                    headlineContent = { Text(exercise.name) },
                    supportingContent = { Text(exercise.type.label()) },
                    leadingContent = {
                        IconAvatar(
                            icon = exercise.type.icon,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    },
                    trailingContent = { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null) },
                    modifier = Modifier
                        .animateItem()
                        .clickable { onOpenExercise(exercise.id) },
                )
            }
        }
    }
}
