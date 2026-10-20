package com.example.tracker.data.local

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Version 9 drops the built-in weight and daily log: their entries become custom stats, so
 * they work like any other. Weight becomes a "Weight" stat, and the daily log's calories,
 * protein and steps a stat each. A stat is only made when it has entries to hold; deleted
 * entries are left behind. The daily log's targets lived in preferences, so
 * [LegacyNutritionTargets] moves them onto these stats once the app starts.
 */
object LegacyLogsMigration : Migration(8, 9) {

    /** A stat made from an old log, under a fixed id so its target can find it. */
    data class LegacyStat(
        val id: String,
        val name: String,
        val unit: String,
        val decimals: Int,
        val icon: String,
        val color: String,
        val table: String,
        val column: String,
    )

    val WEIGHT = LegacyStat("legacy-weight", "Weight", "kg", 1, "SCALE", "BLUE", "body_weight", "weight_kg")
    val CALORIES = LegacyStat("legacy-calories", "Calories", "kcal", 0, "FIRE", "AMBER", "daily_nutrition", "calories")
    val PROTEIN = LegacyStat("legacy-protein", "Protein", "g", 0, "FOOD", "GREEN", "daily_nutrition", "protein_grams")
    val STEPS = LegacyStat("legacy-steps", "Steps", "steps", 0, "STEPS", "TEAL", "daily_nutrition", "steps")

    /** In the order they are listed on the Stats tab. */
    private val STATS = listOf(WEIGHT, CALORIES, PROTEIN, STEPS)

    // The connection overload is the one Room calls with every SQLite driver.
    override fun migrate(connection: SQLiteConnection) {
        val now = System.currentTimeMillis()
        STATS.forEachIndexed { i, stat ->
            val live = "FROM ${stat.table} WHERE deleted_at IS NULL AND ${stat.column} IS NOT NULL"
            // A millisecond apart, so the stats keep this order (they are sorted by creation).
            val createdAt = now + i
            connection.prepare(
                """
                INSERT INTO custom_stats (id, name, unit, decimals, target, icon, color, created_at, updated_at, deleted_at, sync_state)
                SELECT ?, ?, ?, ?, NULL, ?, ?, ?, ?, NULL, 'PENDING' WHERE EXISTS (SELECT 1 $live)
                """,
            ).use { statement ->
                statement.bindText(1, stat.id)
                statement.bindText(2, stat.name)
                statement.bindText(3, stat.unit)
                statement.bindLong(4, stat.decimals.toLong())
                statement.bindText(5, stat.icon)
                statement.bindText(6, stat.color)
                statement.bindLong(7, createdAt)
                statement.bindLong(8, createdAt)
                statement.step()
            }
            connection.prepare(
                """
                INSERT INTO custom_stat_entries (stat_id, epoch_day, value, created_at, updated_at, deleted_at, sync_state)
                SELECT ?, epoch_day, ${stat.column}, created_at, updated_at, NULL, 'PENDING' $live
                """,
            ).use { statement ->
                statement.bindText(1, stat.id)
                statement.step()
            }
        }
        connection.execSQL("DROP TABLE body_weight")
        connection.execSQL("DROP TABLE daily_nutrition")
    }
}
