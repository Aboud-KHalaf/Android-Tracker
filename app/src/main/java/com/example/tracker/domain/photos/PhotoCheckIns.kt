package com.example.tracker.domain.photos

import com.example.tracker.domain.model.PhotoMeasurement
import com.example.tracker.domain.model.PhotoPose
import com.example.tracker.domain.model.ProgressPhoto
import java.time.Instant
import java.time.LocalDate

/**
 * One photo session: the photos taken together (any pose may be missing) and the
 * measurements noted with them. A day can hold several; none replaces another.
 */
data class PhotoCheckIn(
    val id: String,
    val date: LocalDate,
    /** When the session's first photo was kept; orders sessions within a day. */
    val takenAt: Instant,
    val photos: Map<PhotoPose, ProgressPhoto>,
    val measurements: List<PhotoMeasurement> = emptyList(),
)

/** The first and the latest [pose] photo, to show the change between them. */
data class PhotoComparison(val before: ProgressPhoto, val after: ProgressPhoto)

/** Null until [pose] has been photographed in two different check-ins. */
fun comparisonOf(checkIns: List<PhotoCheckIn>, pose: PhotoPose): PhotoComparison? {
    val withPose = checkIns.filter { pose in it.photos }.sortedBy { it.takenAt }
    if (withPose.size < 2) return null
    return PhotoComparison(withPose.first().photos.getValue(pose), withPose.last().photos.getValue(pose))
}

/**
 * The [pose] photo to line the next one up with: the latest one from a check-in other than
 * [currentCheckInId], or the current one's own when that is the only one.
 */
fun guidePhotoFor(checkIns: List<PhotoCheckIn>, pose: PhotoPose, currentCheckInId: String?): ProgressPhoto? {
    val withPose = checkIns.filter { pose in it.photos }
    return withPose.filter { it.id != currentCheckInId }.maxByOrNull { it.takenAt }?.photos?.get(pose)
        ?: withPose.firstOrNull { it.id == currentCheckInId }?.photos?.get(pose)
}

/**
 * How each of [checkIn]'s measurements changed since the latest earlier check-in that has
 * one with the same name and unit; absent when there is none.
 */
fun measurementChanges(checkIns: List<PhotoCheckIn>, checkIn: PhotoCheckIn): Map<PhotoMeasurement, Double> {
    val earlier = checkIns.filter { it.takenAt < checkIn.takenAt }.sortedByDescending { it.takenAt }
    return checkIn.measurements.mapNotNull { measurement ->
        earlier.firstNotNullOfOrNull { previous ->
            previous.measurements.firstOrNull { it.name.equals(measurement.name, ignoreCase = true) && it.unit == measurement.unit }
        }?.let { measurement to measurement.value - it.value }
    }.toMap()
}
