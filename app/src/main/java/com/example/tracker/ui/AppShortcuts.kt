package com.example.tracker.ui

/**
 * The launcher shortcuts (long-press the app icon), declared in res/xml/shortcuts.xml. Each
 * opens the app with one of these intent actions.
 */
object AppShortcuts {
    const val ACTION_START_WORKOUT = "com.example.tracker.action.START_WORKOUT"
    const val ACTION_LOG_STAT = "com.example.tracker.action.LOG_STAT"
    const val ACTION_TAKE_PHOTOS = "com.example.tracker.action.TAKE_PHOTOS"

    fun isShortcut(action: String?): Boolean =
        action == ACTION_START_WORKOUT || action == ACTION_LOG_STAT || action == ACTION_TAKE_PHOTOS
}

/** A screen other than a workout that the app is asked to open from outside. */
enum class AppScreen { STATS, PHOTO_CAPTURE }
