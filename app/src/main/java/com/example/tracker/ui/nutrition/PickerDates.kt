package com.example.tracker.ui.nutrition

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/*
 * Material date pickers work in UTC milliseconds at midnight, whatever the device's time zone.
 * These convert between that and calendar days without shifting the day.
 */

internal fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

internal fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
