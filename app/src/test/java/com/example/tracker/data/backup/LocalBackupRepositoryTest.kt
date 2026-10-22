package com.example.tracker.data.backup

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.example.tracker.data.local.ProgressPhotoStore
import com.example.tracker.data.local.TrackerDatabase
import com.example.tracker.data.repository.FakeTimeProvider
import com.example.tracker.data.repository.OfflineFirstCustomStatRepository
import com.example.tracker.data.repository.OfflineFirstExerciseRepository
import com.example.tracker.data.repository.SequentialIds
import com.example.tracker.data.repository.inMemoryDatabase
import com.example.tracker.domain.model.CustomStat
import com.example.tracker.domain.model.ExerciseType
import com.example.tracker.domain.model.StatColor
import com.example.tracker.domain.model.StatEntry
import com.example.tracker.domain.model.StatIcon
import com.example.tracker.domain.repository.InvalidBackupException
import com.example.tracker.domain.repository.NewerBackupException
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.LocalDate
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LocalBackupRepositoryTest {

    @get:Rule
    val folder = TemporaryFolder()

    @get:Rule
    val migrations = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), TrackerDatabase::class.java)

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val time = FakeTimeProvider()
    private val oct1 = LocalDate.of(2026, 10, 1)
    private val devices = mutableListOf<Device>()

    /** One phone's data: its database, photos and backups. */
    private inner class Device {
        val database = inMemoryDatabase()
        val photosDirectory = folder.newFolder()
        val photos = ProgressPhotoStore(photosDirectory, folder.newFolder())
        val exercises = OfflineFirstExerciseRepository(database, time, SequentialIds())
        val stats = OfflineFirstCustomStatRepository(database, time, SequentialIds())
        val backup = LocalBackupRepository(context, database, photos, File(folder.root, "work-${devices.size}"))

        init {
            devices += this
        }

        suspend fun backUp(): ByteArray = onIoThread {
            ByteArrayOutputStream().also { backup.writeBackup(it) }.toByteArray()
        }

        suspend fun restore(bytes: ByteArray) = onIoThread { backup.restoreBackup(bytes.inputStream()) }

        suspend fun exerciseNames() = exercises.observeExercises().first().map { it.name }

        fun photoNames() = photosDirectory.listFiles().orEmpty().map { it.name }.sorted()
    }

    @After
    fun tearDown() = devices.forEach { it.database.close() }

    @Test
    fun restore_onAnotherDevice_replacesItsDataAndPhotosWithTheBackup() = runTest {
        val phone = Device()
        phone.exercises.createExercise("Bench Press", ExerciseType.WEIGHT_REPS)
        val statId = phone.stats.createStat(CustomStat("", "Weight", "kg", 1, null, StatIcon.SCALE, StatColor.BLUE))
        phone.stats.saveEntry(StatEntry(statId, oct1, 80.5))
        File(phone.photosDirectory, "p1.jpg").writeText("front")
        val newPhone = Device()
        newPhone.exercises.createExercise("Squat", ExerciseType.WEIGHT_REPS)
        File(newPhone.photosDirectory, "old.jpg").writeText("old")

        newPhone.restore(phone.backUp())

        assertEquals(listOf("Bench Press"), newPhone.exerciseNames())
        val stat = newPhone.stats.observeOverviews().first().single()
        assertEquals("Weight", stat.stat.name)
        assertEquals(80.5, stat.latest?.value)
        assertEquals(listOf("p1.jpg"), newPhone.photoNames())
        assertEquals("front", File(newPhone.photosDirectory, "p1.jpg").readText())
    }

    @Test
    fun restore_backupFromAnOlderVersion_isMigrated() = runTest {
        migrations.createDatabase(OLD_DB, 8).use { db ->
            db.execSQL(
                """
                INSERT INTO exercises (id, name, type, created_at, updated_at, deleted_at, sync_state)
                VALUES ('e1', 'Deadlift', 'WEIGHT_REPS', 0, 0, NULL, 'PENDING')
                """
            )
            db.execSQL(
                """
                INSERT INTO body_weight (epoch_day, weight_kg, created_at, updated_at, deleted_at, sync_state)
                VALUES (20000, 79.4, 0, 0, NULL, 'PENDING')
                """
            )
        }
        val device = Device()

        device.restore(zipOf("tracker.db" to context.getDatabasePath(OLD_DB).readBytes()))

        assertEquals(listOf("Deadlift"), device.exerciseNames())
        // Version 9 turned the old weight log into a stat.
        val stat = device.stats.observeOverviews().first().single()
        assertEquals(79.4, stat.latest?.value)
    }

    @Test
    fun restore_notABackup_failsAndKeepsTheData() = runTest {
        val device = Device()
        device.exercises.createExercise("Squat", ExerciseType.WEIGHT_REPS)
        File(device.photosDirectory, "p1.jpg").writeText("front")

        val invalidFiles = listOf(
            "not a zip".toByteArray(),
            zipOf("notes.txt" to "hi".toByteArray()),
            zipOf("tracker.db" to "junk".toByteArray()),
        )
        for (file in invalidFiles) {
            val error = runCatching { device.restore(file) }.exceptionOrNull()
            assertTrue("$error", error is InvalidBackupException)
        }

        assertEquals(listOf("Squat"), device.exerciseNames())
        assertEquals(listOf("p1.jpg"), device.photoNames())
    }

    @Test
    fun restore_backupFromANewerVersion_isRejected() = runTest {
        val newer = folder.newFile("newer.db").apply { delete() }
        SQLiteDatabase.openOrCreateDatabase(newer, null).use { db ->
            db.execSQL("CREATE TABLE room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)")
            db.version = 99
        }
        val device = Device()

        assertThrows(NewerBackupException::class.java) {
            kotlinx.coroutines.runBlocking { device.restore(zipOf("tracker.db" to newer.readBytes())) }
        }
    }

    @Test
    fun restore_ignoresPhotoPathsOutsideThePhotosFolder() = runTest {
        val phone = Device()
        val backup = phone.backUp()
        val device = Device()
        val crafted = ByteArrayOutputStream().also { out ->
            ZipOutputStream(out).use { zip ->
                java.util.zip.ZipInputStream(backup.inputStream()).use { original ->
                    generateSequence { original.nextEntry }.forEach { entry ->
                        zip.putNextEntry(ZipEntry(entry.name))
                        original.copyTo(zip)
                    }
                }
                zip.putNextEntry(ZipEntry("photos/../escaped.jpg"))
                zip.write("x".toByteArray())
            }
        }.toByteArray()

        device.restore(crafted)

        assertTrue(device.photoNames().isEmpty())
        assertFalse(File(folder.root, "escaped.jpg").exists())
    }

    @Test
    fun backUp_leavesNoWorkFilesBehind() = runTest {
        val device = Device()

        device.restore(device.backUp())

        assertFalse(File(folder.root, "work-0").exists())
    }

    private fun zipOf(vararg entries: Pair<String, ByteArray>): ByteArray =
        ByteArrayOutputStream().also { out ->
            ZipOutputStream(out).use { zip ->
                entries.forEach { (name, bytes) ->
                    zip.putNextEntry(ZipEntry(name))
                    zip.write(bytes)
                }
            }
        }.toByteArray()

    /** Room refuses database work on the main thread, which Robolectric tests run on. */
    private suspend fun <T> onIoThread(block: () -> T): T = withContext(Dispatchers.IO) { block() }

    private companion object {
        const val OLD_DB = "old-backup.db"
    }
}
