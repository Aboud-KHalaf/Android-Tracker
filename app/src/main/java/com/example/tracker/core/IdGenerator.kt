package com.example.tracker.core

import java.util.UUID

/**
 * Creates primary keys on the device. Random UUIDs let rows created offline on different
 * devices be synced later without id collisions.
 */
fun interface IdGenerator {
    fun newId(): String
}

object UuidGenerator : IdGenerator {
    override fun newId(): String = UUID.randomUUID().toString()
}
