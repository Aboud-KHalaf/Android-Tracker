package com.example.tracker.ui.history.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.tracker.R
import com.example.tracker.ui.history.PlanFilterUi
import com.example.tracker.ui.theme.spacing

/** "All" plus one chip per plan, scrolling sideways when there are many plans. */
@Composable
fun PlanFilterChips(
    filters: List<PlanFilterUi>,
    selectedPlanId: String?,
    onSelect: (planId: String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val groupDescription = stringResource(R.string.history_filter_group)
    LazyRow(
        modifier = modifier.semantics { contentDescription = groupDescription },
        contentPadding = PaddingValues(horizontal = MaterialTheme.spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
    ) {
        items(filters, key = { it.planId ?: "all" }) { filter ->
            val selected = filter.planId == selectedPlanId
            FilterChip(
                selected = selected,
                onClick = { onSelect(filter.planId) },
                label = { Text(filter.name ?: stringResource(R.string.history_filter_all)) },
                leadingIcon = if (selected) {
                    { Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
                } else {
                    null
                },
            )
        }
    }
}
