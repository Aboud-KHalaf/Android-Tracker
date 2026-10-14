package com.example.tracker.domain.photos

import com.example.tracker.domain.model.PhotoPose
import com.example.tracker.domain.model.ProgressPhoto
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhotoCheckInsTest {

    private val day = LocalDate.of(2026, 10, 1)

    private fun photo(date: LocalDate, pose: PhotoPose) =
        ProgressPhoto(id = "$date-$pose", date = date, pose = pose, filePath = "/photos/$date-$pose.jpg")

    @Test
    fun checkInsOf_groupsByDayNewestFirst() {
        val photos = listOf(
            photo(day, PhotoPose.FRONT),
            photo(day.plusDays(7), PhotoPose.BACK),
            photo(day, PhotoPose.SIDE),
        )

        val checkIns = checkInsOf(photos)

        assertEquals(listOf(day.plusDays(7), day), checkIns.map { it.date })
        assertEquals(setOf(PhotoPose.FRONT, PhotoPose.SIDE), checkIns[1].photos.keys)
    }

    @Test
    fun comparisonOf_usesFirstAndLatestPhotoOfThePose() {
        val first = photo(day, PhotoPose.FRONT)
        val middle = photo(day.plusDays(7), PhotoPose.FRONT)
        val latest = photo(day.plusDays(14), PhotoPose.FRONT)

        val comparison = comparisonOf(listOf(latest, photo(day.plusDays(20), PhotoPose.BACK), middle, first), PhotoPose.FRONT)

        assertEquals(PhotoComparison(first, latest), comparison)
    }

    @Test
    fun comparisonOf_needsTwoDays() {
        assertNull(comparisonOf(listOf(photo(day, PhotoPose.SIDE)), PhotoPose.SIDE))
        assertNull(comparisonOf(listOf(photo(day, PhotoPose.FRONT), photo(day.plusDays(1), PhotoPose.FRONT)), PhotoPose.SIDE))
    }

    @Test
    fun guidePhotoFor_prefersTheLatestEarlierDay() {
        val lastWeek = photo(day.minusDays(7), PhotoPose.SIDE)
        val today = photo(day, PhotoPose.SIDE)

        assertEquals(lastWeek, guidePhotoFor(listOf(today, lastWeek, photo(day.minusDays(14), PhotoPose.SIDE)), PhotoPose.SIDE, day))
    }

    @Test
    fun guidePhotoFor_fallsBackToTodaysPhoto() {
        val today = photo(day, PhotoPose.BACK)

        assertEquals(today, guidePhotoFor(listOf(today), PhotoPose.BACK, day))
        assertNull(guidePhotoFor(listOf(today), PhotoPose.FRONT, day))
    }
}
