package com.example.tracker.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test

class MoveByKeyTest {

    private val items = listOf("a", "b", "c", "d")

    @Test
    fun movesDownAndUp() {
        assertEquals(listOf("b", "c", "a", "d"), items.moveByKey("a", "c") { it })
        assertEquals(listOf("d", "a", "b", "c"), items.moveByKey("d", "a") { it })
    }

    @Test
    fun unknownOrSameKey_leavesTheListAlone() {
        assertEquals(items, items.moveByKey("a", "x") { it })
        assertEquals(items, items.moveByKey("b", "b") { it })
    }
}
