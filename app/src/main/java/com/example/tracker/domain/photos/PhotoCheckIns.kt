package com.example.tracker.domain.photos

import com.example.tracker.domain.model.PhotoPose
import com.example.tracker.domain.model.ProgressPhoto
import java.time.LocalDate

/** The photos taken on one day; any pose may be missing. */
data class PhotoCheckIn(
    val date: LocalDate,
    val photos: Map<PhotoPose, ProgressPhoto>,
)

/** Groups [photos] by day, newest day first. */
fun checkInsOf(photos: List<ProgressPhoto>): List<PhotoCheckIn> =
    photos.groupBy { it.date }
        .map { (date, dayPhotos) -> PhotoCheckIn(date, dayPhotos.associateBy { it.pose }) }
        .sortedByDescending { it.date }

/** The first and the latest [pose] photo, to show the change between them. */
data class PhotoComparison(val before: ProgressPhoto, val after: ProgressPhoto)

/** Null until [pose] has been photographed on two different days. */
fun comparisonOf(photos: List<ProgressPhoto>, pose: PhotoPose): PhotoComparison? {
    val ofPose = photos.filter { it.pose == pose }
    val before = ofPose.minByOrNull { it.date } ?: return null
    val after = ofPose.maxByOrNull { it.date } ?: return null
    return if (before.date == after.date) null else PhotoComparison(before, after)
}

/**
 * The [pose] photo to line the next one up with: the latest one from before [today], or
 * today's own when that is the only one.
 */
fun guidePhotoFor(photos: List<ProgressPhoto>, pose: PhotoPose, today: LocalDate): ProgressPhoto? {
    val ofPose = photos.filter { it.pose == pose }
    return ofPose.filter { it.date.isBefore(today) }.maxByOrNull { it.date }
        ?: ofPose.firstOrNull { it.date == today }
}
