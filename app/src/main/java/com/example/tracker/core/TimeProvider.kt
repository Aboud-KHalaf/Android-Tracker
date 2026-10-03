package com.example.tracker.core

import java.time.Instant
import java.time.ZoneId

/** Source of the current time and time zone, replaceable in tests. */
interface TimeProvider {
    fun now(): Instant
    fun zone(): ZoneId
}

object SystemTimeProvider : TimeProvider {
    override fun now(): Instant = Instant.now()
    override fun zone(): ZoneId = ZoneId.systemDefault()
}
