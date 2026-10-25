package com.example.tracker.data.local

import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Version 15 adds custom_stats.goal. Every existing stat starts as a change over time, which
 * is how all targets were measured before; stats drawn as daily amounts (steps, water,
 * calories, protein, sleep) become daily goals, so a day past the target counts as reached.
 */
class StatGoalMigration : AutoMigrationSpec {
    // The connection overload is the one Room calls with every SQLite driver.
    override fun onPostMigrate(connection: SQLiteConnection) {
        val icons = DAILY_ICONS.joinToString { "'$it'" }
        connection.execSQL("UPDATE custom_stats SET goal = 'DAILY' WHERE icon IN ($icons)")
    }

    companion object {
        val DAILY_ICONS = listOf("STEPS", "WATER", "FIRE", "FOOD", "SLEEP")
    }
}
