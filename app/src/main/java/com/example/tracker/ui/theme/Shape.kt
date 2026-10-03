package com.example.tracker.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Corner radii for the M3 shape scale. Buttons keep the M3 default (fully rounded).
 */
val Shapes = Shapes(
    // Menus, snackbars
    extraSmall = RoundedCornerShape(4.dp),
    // Chips, badges
    small = RoundedCornerShape(8.dp),
    // Set rows, text fields
    medium = RoundedCornerShape(12.dp),
    // Cards, FAB, stepper buttons
    large = RoundedCornerShape(16.dp),
    // Bottom sheets, dialogs
    extraLarge = RoundedCornerShape(28.dp),
)
