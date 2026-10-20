package com.example.tracker.data.local

import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.tracker.core.TimeProvider
import com.example.tracker.data.local.LegacyLogsMigration.LegacyStat
import com.example.tracker.data.local.dao.CustomStatDao
import com.example.tracker.data.local.entity.CustomStatEntity

/**
 * The daily log's calorie, protein and steps targets were kept in preferences. Once, after
 * [LegacyLogsMigration], this sets them on the matching stats (making a stat that had no
 * entries yet) and removes them from preferences. Safe to call on every start.
 */
class LegacyNutritionTargets(
    private val preferences: SharedPreferences,
    private val dao: CustomStatDao,
    private val time: TimeProvider,
) {
    suspend fun moveIntoStats() {
        TARGETS.forEach { (key, stat) ->
            if (!preferences.contains(key)) return@forEach
            val target = preferences.getInt(key, 0).toDouble()
            val existing = dao.getStatIncludingDeleted(stat.id)
            val now = time.now().toEpochMilli()
            when {
                existing == null -> dao.upsertStat(stat.toEntity(target, SyncMetadata.created(now)))
                existing.sync.deletedAt == null && existing.target == null ->
                    dao.upsertStat(existing.copy(target = target, sync = existing.sync.touched(now)))
            }
            preferences.edit(commit = true) { remove(key) }
        }
    }

    private fun LegacyStat.toEntity(target: Double, sync: SyncMetadata) =
        CustomStatEntity(id, name, unit, decimals, target, icon, color, sync)

    private companion object {
        val TARGETS = listOf(
            "calorie_target" to LegacyLogsMigration.CALORIES,
            "protein_target" to LegacyLogsMigration.PROTEIN,
            "steps_target" to LegacyLogsMigration.STEPS,
        )
    }
}
