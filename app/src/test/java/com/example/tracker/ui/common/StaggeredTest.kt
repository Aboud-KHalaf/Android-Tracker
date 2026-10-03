package com.example.tracker.ui.common

import com.example.tracker.ui.common.chart.staggered
import org.junit.Assert.assertEquals
import org.junit.Test

class StaggeredTest {

    @Test
    fun firstItemStartsAtOnce_lastItemStartsAtStagger() {
        assertEquals(0.2f, staggered(progress = 0.1f, index = 0, count = 5), DELTA)
        assertEquals(0f, staggered(progress = 0.5f, index = 4, count = 5), DELTA)
    }

    @Test
    fun everyItemFinishesTogether() {
        (0 until 5).forEach { assertEquals(1f, staggered(progress = 1f, index = it, count = 5), DELTA) }
    }

    @Test
    fun middleItem_isPartWay() {
        // Starts at 0.25, runs for 0.5 of the progress.
        assertEquals(0.5f, staggered(progress = 0.5f, index = 2, count = 5), DELTA)
    }

    @Test
    fun singleItem_followsProgress() {
        assertEquals(0.3f, staggered(progress = 0.3f, index = 0, count = 1), DELTA)
    }

    private companion object {
        const val DELTA = 0.0001f
    }
}
