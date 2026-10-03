package com.example.tracker.ui.plan.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.tracker.R
import com.example.tracker.ui.common.EmptyState

/** Shown in place of the exercise list when the plan has no exercises. */
@Composable
fun PlanEmptyState(modifier: Modifier = Modifier) {
    EmptyState(
        icon = Icons.AutoMirrored.Outlined.PlaylistAdd,
        title = stringResource(R.string.plan_empty_title),
        body = stringResource(R.string.plan_empty_body),
        modifier = modifier,
    )
}
