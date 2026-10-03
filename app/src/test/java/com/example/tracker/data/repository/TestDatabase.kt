package com.example.tracker.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.tracker.core.IdGenerator
import com.example.tracker.core.TimeProvider
import com.example.tracker.data.local.TrackerDatabase
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

internal fun inMemoryDatabase(): TrackerDatabase =
    Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), TrackerDatabase::class.java)
        .allowMainThreadQueries()
        .build()

internal class FakeTimeProvider(var current: Instant = Instant.parse("2026-09-28T16:00:00Z")) : TimeProvider {
    override fun now(): Instant = current
    override fun zone(): ZoneId = ZoneOffset.UTC

    fun advance(duration: Duration) {
        current = current.plus(duration)
    }
}

internal class SequentialIds : IdGenerator {
    private var next = 1
    override fun newId(): String = "id-${next++}"
}
