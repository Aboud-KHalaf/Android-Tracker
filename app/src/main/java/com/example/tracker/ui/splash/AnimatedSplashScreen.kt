package com.example.tracker.ui.splash

import android.app.Activity
import android.os.Build
import android.os.SystemClock
import android.view.animation.AccelerateInterpolator
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.tracker.R

/**
 * Installs the launch splash screen. Call before `super.onCreate`.
 *
 * On API 31+ the icon animation (`avd_splash_icon`) is allowed to finish before the app
 * appears; older versions show a static icon, so there is nothing to wait for. Either way
 * the splash zooms the icon and fades out instead of cutting to the app.
 */
fun Activity.installAnimatedSplashScreen() {
    val splashScreen = installSplashScreen()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val iconDurationMs = resources.getInteger(R.integer.splash_icon_duration_ms)
        val shownAt = SystemClock.uptimeMillis()
        splashScreen.setKeepOnScreenCondition {
            SystemClock.uptimeMillis() - shownAt < iconDurationMs
        }
    }

    splashScreen.setOnExitAnimationListener { provider ->
        val exitDurationMs = resources.getInteger(R.integer.splash_exit_duration_ms).toLong()
        provider.iconView.animate()
            .scaleX(ExitIconScale)
            .scaleY(ExitIconScale)
            .setDuration(exitDurationMs)
            .setInterpolator(AccelerateInterpolator())
            .start()
        provider.view.animate()
            .alpha(0f)
            .setDuration(exitDurationMs)
            .withEndAction { provider.remove() }
            .start()
    }
}

private const val ExitIconScale = 1.6f
