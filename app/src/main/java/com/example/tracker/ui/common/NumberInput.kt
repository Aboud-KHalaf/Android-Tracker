package com.example.tracker.ui.common

/**
 * [this] with Arabic-Indic (٠–٩) and Eastern Arabic-Indic (۰–۹) digits as 0–9 and the Arabic
 * decimal separator (٫) as a dot, dropping the Arabic thousands separator (٬). Arabic and
 * Persian keyboards type those, and Kotlin's number parsing only reads 0–9.
 */
fun String.toWesternDigits(): String = buildString(length) {
    for (char in this@toWesternDigits) {
        when (char) {
            in ARABIC_INDIC_ZERO..ARABIC_INDIC_NINE -> append('0' + (char - ARABIC_INDIC_ZERO))
            in EASTERN_ARABIC_INDIC_ZERO..EASTERN_ARABIC_INDIC_NINE -> append('0' + (char - EASTERN_ARABIC_INDIC_ZERO))
            ARABIC_DECIMAL_SEPARATOR -> append('.')
            ARABIC_THOUSANDS_SEPARATOR -> Unit
            else -> append(char)
        }
    }
}

private const val ARABIC_INDIC_ZERO = '٠'
private const val ARABIC_INDIC_NINE = '٩'
private const val EASTERN_ARABIC_INDIC_ZERO = '۰'
private const val EASTERN_ARABIC_INDIC_NINE = '۹'
private const val ARABIC_DECIMAL_SEPARATOR = '٫'
private const val ARABIC_THOUSANDS_SEPARATOR = '٬'
