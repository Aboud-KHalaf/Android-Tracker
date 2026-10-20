package com.example.tracker.data.local

import android.content.Context
import androidx.core.content.edit
import androidx.test.core.app.ApplicationProvider
import com.example.tracker.data.local.entity.CustomStatEntity
import com.example.tracker.data.repository.FakeTimeProvider
import com.example.tracker.data.repository.inMemoryDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LegacyNutritionTargetsTest {

    private val database = inMemoryDatabase()
    private val dao = database.customStatDao()
    private val preferences = ApplicationProvider.getApplicationContext<Context>()
        .getSharedPreferences("legacy-targets-test", Context.MODE_PRIVATE)
    private val targets = LegacyNutritionTargets(preferences, dao, FakeTimeProvider())

    @After
    fun tearDown() {
        database.close()
        preferences.edit(commit = true) { clear() }
    }

    private suspend fun insertMigratedStat(stat: LegacyLogsMigration.LegacyStat, target: Double? = null) {
        dao.upsertStat(CustomStatEntity(stat.id, stat.name, stat.unit, stat.decimals, target, stat.icon, stat.color, SyncMetadata.created(0)))
    }

    @Test
    fun moveIntoStats_setsTargetsOnMigratedStats_andMakesMissingOnes() = runTest {
        insertMigratedStat(LegacyLogsMigration.CALORIES)
        preferences.edit(commit = true) {
            putInt("calorie_target", 2300)
            putInt("steps_target", 8000)
        }

        targets.moveIntoStats()

        assertEquals(2300.0, dao.getStatIncludingDeleted("legacy-calories")!!.target)
        val steps = dao.getStatIncludingDeleted("legacy-steps")!!
        assertEquals("Steps", steps.name)
        assertEquals(8000.0, steps.target)
        assertNull(dao.getStatIncludingDeleted("legacy-protein"))
        assertFalse(preferences.contains("calorie_target"))
        assertFalse(preferences.contains("steps_target"))
    }

    @Test
    fun moveIntoStats_keepsATargetTheStatAlreadyHas() = runTest {
        insertMigratedStat(LegacyLogsMigration.PROTEIN, target = 140.0)
        preferences.edit(commit = true) { putInt("protein_target", 160) }

        targets.moveIntoStats()

        assertEquals(140.0, dao.getStatIncludingDeleted("legacy-protein")!!.target)
        assertFalse(preferences.contains("protein_target"))
    }
}
