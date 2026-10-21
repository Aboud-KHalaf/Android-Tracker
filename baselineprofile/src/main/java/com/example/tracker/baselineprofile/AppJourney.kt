package com.example.tracker.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until

const val PACKAGE_NAME = "com.example.tracker"
private const val WAIT_MILLIS = 5_000L

/**
 * What most launches look like: open the app, look over Home, then the Progress and Stats tabs.
 * Steps whose screen isn't there (e.g. onboarding on a fresh install) are skipped, so the
 * journey works whatever data the device has.
 */
fun MacrobenchmarkScope.browseMainScreens() {
    pressHome()
    startActivityAndWait()
    device.wait(Until.hasObject(By.res("android:id/content")), WAIT_MILLIS)
    device.findObject(By.scrollable(true))?.let { list ->
        list.fling(Direction.DOWN)
        list.fling(Direction.UP)
    }
    for (tab in listOf("Progress", "Stats", "Home")) {
        device.findObject(By.text(tab))?.click() ?: continue
        device.waitForIdle()
    }
}
