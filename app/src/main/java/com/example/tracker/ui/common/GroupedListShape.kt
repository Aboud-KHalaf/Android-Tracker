package com.example.tracker.ui.common

import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** Gap between rows of a grouped list. */
val GroupedListGap = 2.dp

/**
 * Shape for row [index] of a grouped list of [count] rows: the group as a whole gets the
 * large shape's rounded corners, the rows between are square.
 */
@Composable
@ReadOnlyComposable
fun groupedListItemShape(index: Int, count: Int): Shape {
    val large = MaterialTheme.shapes.large
    val square = CornerSize(0.dp)
    val isFirst = index == 0
    val isLast = index == count - 1
    return large.copy(
        topStart = if (isFirst) large.topStart else square,
        topEnd = if (isFirst) large.topEnd else square,
        bottomEnd = if (isLast) large.bottomEnd else square,
        bottomStart = if (isLast) large.bottomStart else square,
    )
}
