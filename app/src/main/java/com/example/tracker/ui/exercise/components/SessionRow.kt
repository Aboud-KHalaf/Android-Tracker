package com.example.tracker.ui.exercise.components

import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.example.tracker.R
import com.example.tracker.ui.common.DateTile
import com.example.tracker.ui.common.PersonalRecordBadge
import com.example.tracker.ui.common.SetValueUi
import com.example.tracker.ui.common.compactText
import com.example.tracker.ui.common.text
import com.example.tracker.ui.exercise.SessionRowUi

/** One session: its top set or longest hold, every set, and a PB badge when it was one. */
@Composable
fun SessionRow(session: SessionRowUi, modifier: Modifier = Modifier) {
    ListItem(
        modifier = modifier,
        headlineContent = {
            session.best?.let { best ->
                Text(
                    when (best) {
                        is SetValueUi.WeightReps -> stringResource(R.string.exercise_top_set, best.text())
                        is SetValueUi.Hold -> stringResource(R.string.exercise_longest_hold, best.text())
                    },
                )
            }
        },
        supportingContent = { Text(session.sets.compactText()) },
        leadingContent = { DateTile(session.date) },
        trailingContent = if (session.isPersonalBest) {
            { PersonalRecordBadge(stringResource(R.string.exercise_pb_badge)) }
        } else {
            null
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}
