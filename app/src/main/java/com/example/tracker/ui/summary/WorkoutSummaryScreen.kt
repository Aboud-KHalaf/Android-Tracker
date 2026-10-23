package com.example.tracker.ui.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tracker.R
import com.example.tracker.ui.common.ErrorContent
import com.example.tracker.ui.common.LoadingContent
import com.example.tracker.ui.common.MessageContent
import com.example.tracker.ui.common.PersonalRecordBadge
import com.example.tracker.ui.common.SectionHeader
import com.example.tracker.ui.common.StateCrossfade
import com.example.tracker.ui.common.compactText
import com.example.tracker.ui.common.currentLocale
import com.example.tracker.ui.common.formatDuration
import com.example.tracker.ui.common.formatNumber
import com.example.tracker.ui.common.longDate
import com.example.tracker.ui.common.text
import com.example.tracker.ui.theme.Dimens
import com.example.tracker.ui.theme.spacing
import com.example.tracker.ui.theme.tabularNumbers
import kotlin.math.abs

/** Workout summary destination: connects [WorkoutSummaryViewModel] to [WorkoutSummaryScreen]. */
@Composable
fun WorkoutSummaryRoute(
    onBack: () -> Unit,
    onOpenExerciseProgress: (exerciseId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WorkoutSummaryViewModel = viewModel(factory = WorkoutSummaryViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    WorkoutSummaryScreen(
        uiState = uiState,
        onBack = onBack,
        onOpenExerciseProgress = onOpenExerciseProgress,
        onRetry = viewModel::onRetry,
        modifier = modifier,
    )
}

/** Stateless workout summary. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutSummaryScreen(
    uiState: WorkoutSummaryUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenExerciseProgress: (exerciseId: String) -> Unit = {},
    onRetry: () -> Unit = {},
) {
    val success = uiState as? WorkoutSummaryUiState.Success
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(success?.name ?: stringResource(R.string.summary_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
        bottomBar = {
            if (success?.justFinished == true) {
                Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                    Button(
                        onClick = onBack,
                        modifier = Modifier
                            .navigationBarsPadding()
                            .padding(MaterialTheme.spacing.lg)
                            .fillMaxWidth()
                            .height(Dimens.setControlSize),
                    ) { Text(stringResource(R.string.action_done)) }
                }
            }
        },
    ) { innerPadding ->
        StateCrossfade(uiState) { state ->
            when (state) {
                WorkoutSummaryUiState.Loading -> LoadingContent(Modifier.padding(innerPadding))
                WorkoutSummaryUiState.Error -> ErrorContent(
                    message = stringResource(R.string.summary_error),
                    onRetry = onRetry,
                    modifier = Modifier.padding(innerPadding),
                )
                WorkoutSummaryUiState.NotFound ->
                    MessageContent(stringResource(R.string.summary_not_found), Modifier.padding(innerPadding))
                is WorkoutSummaryUiState.Success -> SummaryContent(state, innerPadding, onOpenExerciseProgress)
            }
        }
    }
}

@Composable
private fun SummaryContent(
    state: WorkoutSummaryUiState.Success,
    contentPadding: PaddingValues,
    onOpenExerciseProgress: (String) -> Unit,
) {
    val spacing = MaterialTheme.spacing
    val locale = currentLocale()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = Dimens.maxContentWidth)
                .fillMaxSize(),
            contentPadding = PaddingValues(start = spacing.lg, end = spacing.lg, bottom = spacing.xl),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            item(key = "headline") {
                Column(modifier = Modifier.padding(vertical = spacing.md)) {
                    if (state.justFinished) {
                        Text(
                            stringResource(R.string.summary_done_headline),
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.semantics { heading() },
                        )
                    }
                    Text(
                        state.date.longDate(locale),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            state.note?.let { note ->
                item(key = "note") { NoteCard(label = stringResource(R.string.workout_note), note = note) }
            }
            item(key = "figures") {
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    FigureTile(stringResource(R.string.summary_duration), formatDuration(state.duration), Modifier.weight(1f))
                    FigureTile(stringResource(R.string.summary_sets), state.setCount.toString(), Modifier.weight(1f))
                    FigureTile(
                        label = stringResource(R.string.summary_volume),
                        value = stringResource(R.string.value_kg, formatNumber(state.volumeKg, locale)),
                        detail = state.volumeChangeKg?.let { change -> volumeChangeText(change) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            if (state.personalBests.isNotEmpty()) {
                item(key = "bests-header") {
                    SectionHeader(title = stringResource(R.string.summary_personal_bests), modifier = Modifier.padding(top = spacing.md))
                }
                items(state.personalBests, key = { "best-${it.exerciseName}" }) { best ->
                    ListItem(
                        headlineContent = { Text(best.exerciseName) },
                        supportingContent = { Text(best.value.text(), style = MaterialTheme.typography.bodyMedium.tabularNumbers()) },
                        leadingContent = {
                            Icon(Icons.Outlined.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )
                }
            }
            item(key = "exercises-header") {
                SectionHeader(title = stringResource(R.string.summary_exercises), modifier = Modifier.padding(top = spacing.md))
            }
            items(state.exercises, key = { "exercise-${it.exerciseId}" }) { exercise ->
                Surface(
                    onClick = { onOpenExerciseProgress(exercise.exerciseId) },
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                    ListItem(
                        headlineContent = { Text(exercise.name) },
                        supportingContent = {
                            Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                                if (exercise.sets.isNotEmpty()) {
                                    Text(exercise.sets.compactText(), style = MaterialTheme.typography.bodyMedium.tabularNumbers())
                                }
                                exercise.note?.let { ExerciseNoteText(it) }
                            }
                        },
                        trailingContent = if (exercise.isPersonalBest) {
                            { PersonalRecordBadge(stringResource(R.string.exercise_pb_badge)) }
                        } else {
                            null
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )
                }
            }
        }
    }
}

/** "+250 kg vs last time", "−80 kg vs last time" or "Same as last time". */
@Composable
private fun volumeChangeText(change: Double): String {
    if (change == 0.0) return stringResource(R.string.summary_volume_same)
    val sign = if (change > 0) "+" else "−"
    val amount = stringResource(R.string.value_kg, formatNumber(abs(change), currentLocale()))
    return stringResource(R.string.summary_volume_change, sign + amount)
}

/** A note left on the whole workout, under its label. */
@Composable
private fun NoteCard(label: String, note: String, modifier: Modifier = Modifier) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(MaterialTheme.spacing.md),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
        ) {
            Icon(Icons.AutoMirrored.Outlined.Notes, contentDescription = null)
            Column {
                Text(label, style = MaterialTheme.typography.labelMedium)
                Text(note, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/** The note left on an exercise, set apart from its sets. */
@Composable
private fun ExerciseNoteText(note: String) {
    val description = stringResource(R.string.summary_exercise_note_description, note)
    Row(
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
        modifier = Modifier.clearAndSetSemantics { contentDescription = description },
    ) {
        Icon(
            Icons.AutoMirrored.Outlined.Notes,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(Dimens.inlineIconSize),
        )
        Text(
            note,
            style = MaterialTheme.typography.bodyMedium,
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FigureTile(label: String, value: String, modifier: Modifier = Modifier, detail: String? = null) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.md)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge.tabularNumbers(), maxLines = 1)
            detail?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Start,
                )
            }
        }
    }
}
