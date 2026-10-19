package com.example.tracker.domain.photos

import com.example.tracker.domain.model.PhotoMeasurement
import com.example.tracker.domain.model.PhotoPose
import com.example.tracker.domain.model.ProgressPhoto
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhotoCheckInsTest {

    private val day = LocalDate.of(2026, 10, 1)

    /** A check-in [daysLater] days (plus [hoursLater] hours) after [day], with photos of [poses]. */
    private fun checkIn(
        id: String,
        daysLater: Long,
        vararg poses: PhotoPose,
        hoursLater: Long = 0,
        measurements: List<PhotoMeasurement> = emptyList(),
    ): PhotoCheckIn {
        val date = day.plusDays(daysLater)
        val takenAt: Instant = date.atStartOfDay().toInstant(ZoneOffset.UTC).plus(Duration.ofHours(8 + hoursLater))
        return PhotoCheckIn(
            id = id,
            date = date,
            takenAt = takenAt,
            photos = poses.associateWith { ProgressPhoto("$id-$it", id, date, it, "/photos/$id-$it.jpg") },
            measurements = measurements,
        )
    }

    @Test
    fun comparisonOf_usesFirstAndLatestCheckInWithThePose() {
        val first = checkIn("a", 0, PhotoPose.FRONT)
        val middle = checkIn("b", 7, PhotoPose.FRONT)
        val latest = checkIn("c", 14, PhotoPose.FRONT)

        val comparison = comparisonOf(listOf(latest, checkIn("d", 20, PhotoPose.BACK), middle, first), PhotoPose.FRONT)

        assertEquals(PhotoComparison(first.photos.getValue(PhotoPose.FRONT), latest.photos.getValue(PhotoPose.FRONT)), comparison)
    }

    @Test
    fun comparisonOf_twoCheckInsTheSameDay_compare() {
        val morning = checkIn("a", 0, PhotoPose.SIDE)
        val evening = checkIn("b", 0, PhotoPose.SIDE, hoursLater = 10)

        assertEquals(
            PhotoComparison(morning.photos.getValue(PhotoPose.SIDE), evening.photos.getValue(PhotoPose.SIDE)),
            comparisonOf(listOf(evening, morning), PhotoPose.SIDE),
        )
    }

    @Test
    fun comparisonOf_needsTwoCheckInsWithThePose() {
        assertNull(comparisonOf(listOf(checkIn("a", 0, PhotoPose.SIDE)), PhotoPose.SIDE))
        assertNull(comparisonOf(listOf(checkIn("a", 0, PhotoPose.FRONT), checkIn("b", 1, PhotoPose.FRONT)), PhotoPose.SIDE))
    }

    @Test
    fun guidePhotoFor_prefersTheLatestOtherCheckIn() {
        val lastWeek = checkIn("old", 0, PhotoPose.SIDE)
        val earlierToday = checkIn("earlier", 7, PhotoPose.SIDE)
        val current = checkIn("current", 7, PhotoPose.SIDE, hoursLater = 2)

        val guide = guidePhotoFor(listOf(current, earlierToday, lastWeek), PhotoPose.SIDE, currentCheckInId = "current")

        assertEquals(earlierToday.photos.getValue(PhotoPose.SIDE), guide)
    }

    @Test
    fun guidePhotoFor_fallsBackToTheCurrentCheckIn_orNothing() {
        val current = checkIn("current", 0, PhotoPose.BACK)

        assertEquals(current.photos.getValue(PhotoPose.BACK), guidePhotoFor(listOf(current), PhotoPose.BACK, "current"))
        assertNull(guidePhotoFor(listOf(current), PhotoPose.FRONT, "current"))
        assertNull(guidePhotoFor(emptyList(), PhotoPose.FRONT, null))
    }

    @Test
    fun measurementChanges_compareWithTheLatestEarlierCheckInThatHasIt() {
        val oldest = checkIn("a", 0, PhotoPose.FRONT, measurements = listOf(PhotoMeasurement("Waist", "cm", 90.0)))
        val middle = checkIn(
            "b",
            7,
            PhotoPose.FRONT,
            measurements = listOf(PhotoMeasurement("Waist", "cm", 88.0), PhotoMeasurement("Weight", "kg", 80.0)),
        )
        val noWaist = checkIn("c", 10, PhotoPose.FRONT, measurements = listOf(PhotoMeasurement("Weight", "kg", 79.0)))
        val latestWaist = PhotoMeasurement("waist", "cm", 86.5)
        val latestChest = PhotoMeasurement("Chest", "cm", 100.0)
        val latest = checkIn("d", 14, PhotoPose.FRONT, measurements = listOf(latestWaist, latestChest))

        val changes = measurementChanges(listOf(latest, noWaist, middle, oldest), latest)

        assertEquals(mapOf(latestWaist to -1.5), changes)
    }
}
