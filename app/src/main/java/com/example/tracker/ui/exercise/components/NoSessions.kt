package com.example.tracker.ui.exercise.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.tracker.R
import com.example.tracker.ui.common.EmptyState

/** Shown before the exercise has been done in any finished workout. */
@Composable
fun NoSessions(modifier: Modifier = Modifier) {
    EmptyState(
        icon = Icons.Outlined.Insights,
        title = stringResource(R.string.exercise_no_sessions_title),
        body = stringResource(R.string.exercise_no_sessions_body),
        modifier = modifier,
    )
}
