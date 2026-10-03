package com.example.tracker.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.example.tracker.R

private val googleFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private val robotoFlex = GoogleFont("Roboto Flex")

/**
 * Roboto Flex, downloaded from Google Fonts at runtime. Compose renders with the platform
 * default ([FontFamily.Default]) while the font loads or if the provider is unavailable.
 */
private val RobotoFlex = FontFamily(
    Font(googleFont = robotoFlex, fontProvider = googleFontProvider, weight = FontWeight.Normal),
    Font(googleFont = robotoFlex, fontProvider = googleFontProvider, weight = FontWeight.Medium),
    Font(googleFont = robotoFlex, fontProvider = googleFontProvider, weight = FontWeight.SemiBold),
)


private val baseline = Typography()

/** M3 type scale with default sizes, line heights and letter spacing, set in Roboto Flex. */
val Typography = Typography(
    displayLarge = baseline.displayLarge.copy(fontFamily = RobotoFlex),
    displayMedium = baseline.displayMedium.copy(fontFamily = RobotoFlex),
    displaySmall = baseline.displaySmall.copy(fontFamily = RobotoFlex),
    headlineLarge = baseline.headlineLarge.copy(fontFamily = RobotoFlex),
    headlineMedium = baseline.headlineMedium.copy(fontFamily = RobotoFlex),
    headlineSmall = baseline.headlineSmall.copy(fontFamily = RobotoFlex),
    titleLarge = baseline.titleLarge.copy(fontFamily = RobotoFlex),
    titleMedium = baseline.titleMedium.copy(fontFamily = RobotoFlex),
    titleSmall = baseline.titleSmall.copy(fontFamily = RobotoFlex),
    bodyLarge = baseline.bodyLarge.copy(fontFamily = RobotoFlex),
    bodyMedium = baseline.bodyMedium.copy(fontFamily = RobotoFlex),
    bodySmall = baseline.bodySmall.copy(fontFamily = RobotoFlex),
    labelLarge = baseline.labelLarge.copy(fontFamily = RobotoFlex),
    labelMedium = baseline.labelMedium.copy(fontFamily = RobotoFlex),
    labelSmall = baseline.labelSmall.copy(fontFamily = RobotoFlex),
)

/**
 * Tabular (fixed-width) digits, so numbers that change in place (weights, reps, timers)
 * don't shift horizontally. Usage: `MaterialTheme.typography.displayMedium.tabularNumbers()`.
 */
fun TextStyle.tabularNumbers(): TextStyle = copy(fontFeatureSettings = "tnum")
