package com.example.tracker.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tracker.R
import com.example.tracker.domain.model.CustomStat
import com.example.tracker.ui.common.ObserveAsEvents
import com.example.tracker.ui.common.sharedAxisX
import com.example.tracker.ui.program.ProgramSetup
import com.example.tracker.ui.program.components.ProgramSetupForm
import com.example.tracker.ui.stats.StatTemplate
import com.example.tracker.ui.theme.Dimens
import com.example.tracker.ui.theme.spacing

/** Onboarding destination: connects [OnboardingViewModel] to [OnboardingScreen]. */
@Composable
fun OnboardingRoute(
    onFinished: (programId: String?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = viewModel(factory = OnboardingViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is OnboardingEvent.Finished -> onFinished(event.programId)
            is OnboardingEvent.ShowMessage -> snackbarHostState.showSnackbar(context.getString(event.messageRes))
        }
    }
    // Back steps back through onboarding; from the welcome it leaves, to come back next launch.
    BackHandler(enabled = uiState.step != OnboardingStep.WELCOME, onBack = viewModel::onBack)
    OnboardingScreen(
        uiState = uiState,
        actions = OnboardingActions(
            onGetStarted = viewModel::onGetStarted,
            onSkipAll = viewModel::onSkipAll,
            onCreateProgram = viewModel::onCreateProgram,
            onSkipProgram = viewModel::onSkipProgram,
            onFinish = viewModel::onFinish,
        ),
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

data class OnboardingActions(
    val onGetStarted: () -> Unit = {},
    val onSkipAll: () -> Unit = {},
    val onCreateProgram: (ProgramSetup) -> Unit = {},
    val onSkipProgram: () -> Unit = {},
    val onFinish: (List<CustomStat>) -> Unit = {},
)

/** Stateless onboarding: one step at a time, sliding forward as the user goes on. */
@Composable
fun OnboardingScreen(
    uiState: OnboardingUiState,
    actions: OnboardingActions,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(modifier = modifier, snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
        AnimatedContent(
            targetState = uiState.step,
            transitionSpec = { sharedAxisX(forward = targetState > initialState) },
            label = "onboarding-step",
            modifier = Modifier.padding(innerPadding),
        ) { step ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                Column(
                    modifier = Modifier
                        .widthIn(max = Dimens.maxContentWidth)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(MaterialTheme.spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.lg),
                ) {
                    when (step) {
                        OnboardingStep.WELCOME -> WelcomeStep(actions)
                        OnboardingStep.PROGRAM -> ProgramStep(actions)
                        OnboardingStep.STATS -> StatsStep(uiState.isSaving, actions)
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeStep(actions: OnboardingActions) {
    StepTitle(stringResource(R.string.onboarding_welcome_title), stringResource(R.string.onboarding_welcome_body))
    Feature(Icons.Outlined.CalendarMonth, stringResource(R.string.onboarding_feature_plan))
    Feature(Icons.Outlined.Insights, stringResource(R.string.onboarding_feature_progress))
    Feature(Icons.Outlined.QueryStats, stringResource(R.string.onboarding_feature_stats))
    Feature(Icons.Outlined.Lock, stringResource(R.string.onboarding_feature_private))
    Button(
        onClick = actions.onGetStarted,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.minTouchTarget),
    ) { Text(stringResource(R.string.onboarding_get_started)) }
    TextButton(onClick = actions.onSkipAll, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.onboarding_skip))
    }
}

@Composable
private fun ProgramStep(actions: OnboardingActions) {
    StepTitle(stringResource(R.string.onboarding_program_title), stringResource(R.string.onboarding_program_body))
    ProgramSetupForm(submitLabel = stringResource(R.string.create_program_confirm), onSubmit = actions.onCreateProgram)
    TextButton(onClick = actions.onSkipProgram, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.onboarding_not_now))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatsStep(isSaving: Boolean, actions: OnboardingActions) {
    val context = LocalContext.current
    var chosen by rememberSaveable { mutableStateOf(listOf(StatTemplate.WEIGHT.name)) }
    StepTitle(stringResource(R.string.onboarding_stats_title), stringResource(R.string.onboarding_stats_body))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
        StatTemplate.entries.forEach { template ->
            val selected = template.name in chosen
            FilterChip(
                selected = selected,
                onClick = { chosen = if (selected) chosen - template.name else chosen + template.name },
                label = { Text(stringResource(template.nameRes)) },
            )
        }
    }
    Button(
        onClick = {
            actions.onFinish(
                StatTemplate.entries.filter { it.name in chosen }.map { template ->
                    CustomStat(
                        id = "",
                        name = context.getString(template.nameRes),
                        unit = context.getString(template.unitRes),
                        decimals = template.decimals,
                        target = null,
                        icon = template.icon,
                        color = template.color,
                        goal = template.goal,
                    )
                },
            )
        },
        enabled = !isSaving,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.minTouchTarget),
    ) { Text(stringResource(R.string.onboarding_finish)) }
}

@Composable
private fun StepTitle(title: String, body: String) {
    Column(
        modifier = Modifier.padding(top = MaterialTheme.spacing.xl),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
        Text(body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Feature(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}
