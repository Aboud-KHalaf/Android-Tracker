package com.example.tracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.tracker.domain.model.StatColor

/** A tonal pair: a container and the content drawn on it, like M3's `*Container` roles. */
@Immutable
data class AccentColors(val container: Color, val content: Color)

/** The accent of each [StatColor] in the current light or dark scheme. Access via `MaterialTheme.statColors`. */
@Immutable
class StatPalette internal constructor(private val colors: Map<StatColor, AccentColors>) {
    operator fun get(color: StatColor): AccentColors = colors.getValue(color)
}

// Each hue's tone-90 container with tone-30 content in light, and the reverse in dark.
private val tones = mapOf(
    StatColor.BLUE to (Color(0xFFD6E3FF) to Color(0xFF234A86)),
    StatColor.TEAL to (Color(0xFFA0F0E6) to Color(0xFF00504A)),
    StatColor.GREEN to (Color(0xFFC3EFAD) to Color(0xFF2B5016)),
    StatColor.AMBER to (Color(0xFFFFDF9E) to Color(0xFF5B4300)),
    StatColor.ORANGE to (Color(0xFFFFDBCA) to Color(0xFF723600)),
    StatColor.RED to (Color(0xFFFFDAD6) to Color(0xFF93000A)),
    StatColor.PINK to (Color(0xFFFFD8E8) to Color(0xFF7B2950)),
    StatColor.PURPLE to (Color(0xFFEADDFF) to Color(0xFF4F378B)),
)

internal val LightStatPalette = StatPalette(tones.mapValues { (_, t) -> AccentColors(container = t.first, content = t.second) })
internal val DarkStatPalette = StatPalette(tones.mapValues { (_, t) -> AccentColors(container = t.second, content = t.first) })

internal val LocalStatPalette = staticCompositionLocalOf { LightStatPalette }

val MaterialTheme.statColors: StatPalette
    @Composable
    @ReadOnlyComposable
    get() = LocalStatPalette.current
