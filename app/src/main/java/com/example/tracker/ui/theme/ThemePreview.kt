package com.example.tracker.ui.theme

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/*
 * Theme showcase for checking tokens against the "Theme tokens" artboard.
 * Preview-only: not used by any screen.
 */

@Composable
private fun ThemeShowcase() {
    val spacing = MaterialTheme.spacing
    Column(
        modifier = Modifier.padding(spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.xl)
    ) {
        ColorSwatches()
        TypeScale()
        Components()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorSwatches() {
    val c = MaterialTheme.colorScheme
    val roles = listOf(
        "primary" to c.primary,
        "onPrimary" to c.onPrimary,
        "primaryContainer" to c.primaryContainer,
        "onPrimaryContainer" to c.onPrimaryContainer,
        "secondary" to c.secondary,
        "secondaryContainer" to c.secondaryContainer,
        "tertiary" to c.tertiary,
        "tertiaryContainer" to c.tertiaryContainer,
        "error" to c.error,
        "errorContainer" to c.errorContainer,
        "surface" to c.surface,
        "surfaceDim" to c.surfaceDim,
        "surfaceBright" to c.surfaceBright,
        "surfaceContainerLowest" to c.surfaceContainerLowest,
        "surfaceContainerLow" to c.surfaceContainerLow,
        "surfaceContainer" to c.surfaceContainer,
        "surfaceContainerHigh" to c.surfaceContainerHigh,
        "surfaceContainerHighest" to c.surfaceContainerHighest,
        "surfaceVariant" to c.surfaceVariant,
        "outline" to c.outline,
        "outlineVariant" to c.outlineVariant,
        "inverseSurface" to c.inverseSurface,
        "inversePrimary" to c.inversePrimary,
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)
    ) {
        roles.forEach { (name, color) -> Swatch(name, color) }
    }
}

@Composable
private fun Swatch(name: String, color: Color) {
    Column(modifier = Modifier.width(96.dp)) {
        Box(
            modifier = Modifier
                .size(width = 96.dp, height = 40.dp)
                .background(color, MaterialTheme.shapes.small)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.small)
        )
        Text(name, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun TypeScale() {
    val t = MaterialTheme.typography
    val styles: List<Pair<String, TextStyle>> = listOf(
        "displayMedium 1:23.4" to t.displayMedium.tabularNumbers(),
        "displaySmall 102.5" to t.displaySmall.tabularNumbers(),
        "headlineMedium" to t.headlineMedium,
        "headlineSmall" to t.headlineSmall,
        "titleLarge" to t.titleLarge,
        "titleMedium" to t.titleMedium,
        "bodyLarge" to t.bodyLarge,
        "bodyMedium" to t.bodyMedium,
        "labelLarge" to t.labelLarge,
        "labelMedium" to t.labelMedium,
        "labelSmall" to t.labelSmall,
    )
    Column {
        styles.forEach { (label, style) -> Text(label, style = style) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Components() {
    val spacing = MaterialTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            Button(onClick = {}) { Text("Filled") }
            FilledTonalButton(onClick = {}) { Text("Tonal") }
            OutlinedButton(onClick = {}) { Text("Outlined") }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            FilterChip(selected = true, onClick = {}, label = { Text("Selected") })
            FilterChip(selected = false, onClick = {}, label = { Text("Unselected") })
        }
        Card {
            Column(modifier = Modifier.padding(spacing.lg)) {
                Text("Bench press", style = MaterialTheme.typography.titleMedium)
                Text(
                    "3 × 8 @ 80 kg",
                    style = MaterialTheme.typography.bodyMedium.tabularNumbers(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(name = "Light", widthDp = 420, heightDp = 1400)
@Composable
private fun ThemeShowcaseLightPreview() {
    TrackerTheme(darkTheme = false) {
        Surface { ThemeShowcase() }
    }
}

@Preview(name = "Dark", widthDp = 420, heightDp = 1400, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ThemeShowcaseDarkPreview() {
    TrackerTheme(darkTheme = true) {
        Surface { ThemeShowcase() }
    }
}
