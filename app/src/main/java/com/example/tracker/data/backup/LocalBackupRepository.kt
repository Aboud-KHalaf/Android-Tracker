package com.example.tracker.data.backup

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import androidx.core.net.toUri
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.tracker.data.local.ProgressPhotoStore
import com.example.tracker.data.local.TrackerDatabase
import com.example.tracker.domain.repository.BackupRepository
import com.example.tracker.domain.repository.InvalidBackupException
import com.example.tracker.domain.repository.NewerBackupException
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A backup is a zip file holding a copy of the database ([DATABASE_ENTRY]) and every progress
 * photo (under [PHOTOS_FOLDER]).
 *
 * The database copy is an ordinary database of the version that made it, so restoring an
 * older backup runs the same migrations as updating the app. Rows are copied table by table,
 * by column name, so nothing here has to change when tables do.
 */
class LocalBackupRepository(
    context: Context,
    private val database: TrackerDatabase,
    private val photoStore: ProgressPhotoStore,
    /** An empty folder of its own to unpack into; cleared before and after each use. */
    private val workDirectory: File,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : BackupRepository {
    private val context = context.applicationContext

    override suspend fun backUp(destination: String) = withContext(ioDispatcher) {
        val output = context.contentResolver.openOutputStream(destination.toUri(), "wt")
            ?: throw IOException("Can't open $destination")
        output.use(::writeBackup)
    }

    override suspend fun restore(source: String) = withContext(ioDispatcher) {
        val input = context.contentResolver.openInputStream(source.toUri())
            ?: throw IOException("Can't open $source")
        input.use(::restoreBackup)
    }

    internal fun writeBackup(output: OutputStream) = inWorkDirectory { directory ->
        val databaseFile = File(directory, DATABASE_ENTRY)
        val copy = TrackerDatabase.open(context, databaseFile)
        try {
            copyAllRows(from = database, to = copy)
        } finally {
            copy.close()
        }
        ZipOutputStream(output.buffered()).use { zip ->
            zip.putFile(DATABASE_ENTRY, databaseFile)
            photoStore.files().forEach { zip.putFile(PHOTOS_FOLDER + it.name, it) }
        }
    }

    /** Unpacks and checks everything before touching the app's data, so a bad file changes nothing. */
    internal fun restoreBackup(input: InputStream) = inWorkDirectory { directory ->
        val databaseFile = File(directory, DATABASE_ENTRY)
        val photosDirectory = File(directory, PHOTOS_FOLDER)
        unzip(input, databaseFile, photosDirectory)
        checkVersion(databaseFile)
        val backup = try {
            TrackerDatabase.open(context, databaseFile).also { it.openHelper.writableDatabase }
        } catch (e: IllegalStateException) {
            // Room couldn't migrate it or it doesn't match the expected tables.
            throw InvalidBackupException(e)
        } catch (e: SQLiteException) {
            throw InvalidBackupException(e)
        }
        try {
            copyAllRows(from = backup, to = database)
        } finally {
            backup.close()
        }
        photoStore.replaceAll(photosDirectory)
    }

    private inline fun <T> inWorkDirectory(block: (File) -> T): T {
        workDirectory.deleteRecursively()
        if (!workDirectory.mkdirs()) throw IOException("Can't create $workDirectory")
        try {
            return block(workDirectory)
        } finally {
            workDirectory.deleteRecursively()
        }
    }

    /** Keeps only the database and plainly named photos; anything else in the zip is ignored. */
    private fun unzip(input: InputStream, databaseFile: File, photosDirectory: File) {
        val hasDatabase = try {
            ZipInputStream(input.buffered()).use { zip -> extractEntries(zip, databaseFile, photosDirectory) }
        } catch (e: ZipException) {
            throw InvalidBackupException(e)
        }
        if (!hasDatabase) throw InvalidBackupException()
    }

    /** Writes each entry to its [entryTarget]; true if the database was among them. */
    private fun extractEntries(zip: ZipInputStream, databaseFile: File, photosDirectory: File): Boolean {
        var hasDatabase = false
        generateSequence { zip.nextEntry }.filterNot { it.isDirectory }.forEach { entry ->
            val target = entryTarget(entry.name, databaseFile, photosDirectory) ?: return@forEach
            if (target == databaseFile) hasDatabase = true
            target.outputStream().use { zip.copyTo(it) }
        }
        return hasDatabase
    }

    /** Where a backup entry goes: the database, a progress photo, or nowhere for anything else. */
    private fun entryTarget(name: String, databaseFile: File, photosDirectory: File): File? = when {
        name == DATABASE_ENTRY -> databaseFile
        name.startsWith(PHOTOS_FOLDER) -> name.removePrefix(PHOTOS_FOLDER)
            .takeIf { it.matches(PHOTO_NAME) }
            ?.let { File(photosDirectory.apply { mkdirs() }, it) }
        else -> null
    }

    /** Rejects files that aren't this app's database, and backups from a newer version. */
    private fun checkVersion(databaseFile: File) {
        val version = schemaVersion(databaseFile) ?: throw InvalidBackupException()
        if (version > database.openHelper.readableDatabase.version) throw NewerBackupException()
    }

    /** The Room schema version of [databaseFile]; null if it isn't a Room database. */
    private fun schemaVersion(databaseFile: File): Int? = try {
        SQLiteDatabase.openDatabase(databaseFile.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            val isRoom = DatabaseUtils.longForQuery(
                db,
                "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = 'room_master_table'",
                null,
            ) > 0
            db.version.takeIf { isRoom && it >= 1 }
        }
    } catch (e: SQLiteException) {
        throw InvalidBackupException(e)
    }

    /**
     * Replaces every row in [to] with the rows of [from], in one transaction on each, so readers
     * of [to] see either the old data or the new, and [from] is read as one snapshot.
     */
    private fun copyAllRows(from: TrackerDatabase, to: TrackerDatabase) {
        from.runInTransaction(Runnable {
            to.runInTransaction(Runnable {
                val source = from.openHelper.writableDatabase
                val target = to.openHelper.writableDatabase
                // Rows arrive table by table, so references are checked once all are in.
                target.execSQL("PRAGMA defer_foreign_keys = ON")
                val tables = target.dataTables()
                tables.forEach { target.delete(it, null, null) }
                tables.forEach { table -> copyTable(table, source, target) }
            })
        })
    }

    private fun copyTable(table: String, source: SupportSQLiteDatabase, target: SupportSQLiteDatabase) {
        val columns = target.columns(table)
        val select = "SELECT ${columns.joinToString { "`$it`" }} FROM `$table`"
        source.query(select).use { cursor ->
            while (cursor.moveToNext()) {
                val values = ContentValues(columns.size)
                columns.forEachIndexed { index, column -> values.put(column, cursor, index) }
                target.insert(table, SQLiteDatabase.CONFLICT_ABORT, values)
            }
        }
    }

    private fun SupportSQLiteDatabase.dataTables(): List<String> =
        query(
            "SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite\\_%' ESCAPE '\\' " +
                "AND name NOT IN ('android_metadata', 'room_master_table')",
        ).use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.getString(0)) } }

    private fun SupportSQLiteDatabase.columns(table: String): List<String> =
        query("PRAGMA table_info(`$table`)").use { cursor ->
            val name = cursor.getColumnIndexOrThrow("name")
            buildList { while (cursor.moveToNext()) add(cursor.getString(name)) }
        }

    private fun ContentValues.put(column: String, cursor: Cursor, index: Int) {
        when (cursor.getType(index)) {
            Cursor.FIELD_TYPE_NULL -> putNull(column)
            Cursor.FIELD_TYPE_INTEGER -> put(column, cursor.getLong(index))
            Cursor.FIELD_TYPE_FLOAT -> put(column, cursor.getDouble(index))
            Cursor.FIELD_TYPE_BLOB -> put(column, cursor.getBlob(index))
            else -> put(column, cursor.getString(index))
        }
    }

    private fun ZipOutputStream.putFile(name: String, file: File) {
        putNextEntry(ZipEntry(name))
        file.inputStream().use { it.copyTo(this) }
        closeEntry()
    }

    companion object {
        /** Folder name under the app's cache folder for [workDirectory]. */
        const val WORK_FOLDER = "backup"
        private const val DATABASE_ENTRY = "tracker.db"
        private const val PHOTOS_FOLDER = "photos/"

        /** Photo files are named after their id; this also keeps paths like "../x" out. */
        private val PHOTO_NAME = Regex("[A-Za-z0-9_-][A-Za-z0-9._-]*")
    }
}
