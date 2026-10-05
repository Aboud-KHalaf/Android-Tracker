package com.example.tracker.ui.catalog.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.SubcomposeAsyncImage
import com.example.tracker.domain.model.ExerciseType
import com.example.tracker.ui.common.icon

/**
 * A catalog exercise's picture, or its type's icon while loading, on failure or when the
 * catalog has none. Size it through [modifier].
 */
@Composable
fun CatalogExerciseImage(
    imageUrl: String?,
    type: ExerciseType,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    val container = modifier
        .clip(MaterialTheme.shapes.medium)
        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
    if (imageUrl == null) {
        TypePlaceholder(type, container)
    } else {
        SubcomposeAsyncImage(
            model = imageUrl,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            loading = { TypePlaceholder(type) },
            error = { TypePlaceholder(type) },
            modifier = container,
        )
    }
}

@Composable
private fun TypePlaceholder(type: ExerciseType, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Icon(type.icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
