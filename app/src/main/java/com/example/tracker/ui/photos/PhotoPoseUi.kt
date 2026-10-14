package com.example.tracker.ui.photos

import androidx.annotation.StringRes
import com.example.tracker.R
import com.example.tracker.domain.model.PhotoPose

@get:StringRes
val PhotoPose.labelRes: Int
    get() = when (this) {
        PhotoPose.FRONT -> R.string.photos_pose_front
        PhotoPose.SIDE -> R.string.photos_pose_side
        PhotoPose.BACK -> R.string.photos_pose_back
    }

/** How to stand for this pose. */
@get:StringRes
val PhotoPose.hintRes: Int
    get() = when (this) {
        PhotoPose.FRONT -> R.string.capture_hint_front
        PhotoPose.SIDE -> R.string.capture_hint_side
        PhotoPose.BACK -> R.string.capture_hint_back
    }
