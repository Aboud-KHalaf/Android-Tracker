package com.example.tracker.data.local

import java.io.File
import java.io.IOException

/**
 * The picture files behind progress photos. Kept photos live in [photosDirectory]; new
 * captures start in [capturesDirectory] (a cache folder) until they are kept.
 */
class ProgressPhotoStore(
    private val photosDirectory: File,
    private val capturesDirectory: File,
) {
    fun file(fileName: String): File = File(photosDirectory, fileName)

    /** Only one capture is pending at a time, so this also clears any a closed screen left behind. */
    fun newCaptureFile(): File {
        capturesDirectory.deleteRecursively()
        capturesDirectory.mkdirs()
        return File.createTempFile("capture-", ".jpg", capturesDirectory)
    }

    /** Moves [capture] into the photos folder as [fileName]. */
    fun keep(capture: File, fileName: String) {
        if (!photosDirectory.isDirectory && !photosDirectory.mkdirs()) throw IOException("Can't create $photosDirectory")
        val target = file(fileName)
        // Renaming is instant; copying covers folders on different volumes.
        if (!capture.renameTo(target)) {
            capture.copyTo(target, overwrite = true)
            capture.delete()
        }
    }

    fun delete(fileName: String) {
        file(fileName).delete()
    }

    fun discard(capture: File) {
        capture.delete()
    }

    /** Every kept photo's file. */
    fun files(): List<File> = photosDirectory.listFiles()?.filter { it.isFile }.orEmpty()

    /** Replaces every kept photo with the files in [directory], which is moved, not copied. */
    fun replaceAll(directory: File) {
        photosDirectory.deleteRecursively()
        if (!directory.isDirectory) return
        // Renaming is instant; copying covers folders on different volumes.
        if (!directory.renameTo(photosDirectory)) {
            directory.copyRecursively(photosDirectory, overwrite = true)
            directory.deleteRecursively()
        }
    }

    /** Deletes every photo and capture. */
    fun deleteAll() {
        photosDirectory.deleteRecursively()
        capturesDirectory.deleteRecursively()
    }

    companion object {
        /** Folder name under the app's files folder; excluded from backups (see res/xml). */
        const val PHOTOS_FOLDER = "progress_photos"
        const val CAPTURES_FOLDER = "photo_captures"
    }
}
