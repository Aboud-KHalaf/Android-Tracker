package com.example.tracker.ui.nutrition

import com.example.tracker.ui.common.formatNumber
import java.util.Locale

/** "2,300": a whole calorie or gram amount, grouped. */
fun formatAmount(value: Int, locale: Locale): String = formatNumber(value.toDouble(), locale)
