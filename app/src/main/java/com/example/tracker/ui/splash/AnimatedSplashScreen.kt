package com.example.tracker.ui.splash

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Activity
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.view.View
import android.view.animation.PathInterpolator
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.splashscreen.SplashScreenViewProvider
import com.example.tracker.R

/**
 * Installs the launch splash screen. Call before `super.onCreate`.
 *
 * On API 31+ the icon animation (`avd_splash_icon`) is allowed to finish before the app
 * appears; older versions show a static icon, so there is nothing to wait for. Either way the
 * icon dips, then zooms out of view while the background fades a moment later, so the app is
 * revealed rather than cut to. With animations turned off in the system settings the splash
 * neither waits nor animates.
 */
fun Activity.installAnimatedSplashScreen() {
    val splashScreen = installSplashScreen()
    if (!animationsEnabled()) return

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val iconDurationMs = resources.getInteger(R.integer.splash_icon_duration_ms)
        val shownAt = SystemClock.uptimeMillis()
        splashScreen.setKeepOnScreenCondition {
            SystemClock.uptimeMillis() - shownAt < iconDurationMs
        }
    }

    splashScreen.setOnExitAnimationListener { provider ->
        val exitDurationMs = resources.getInteger(R.integer.splash_exit_duration_ms).toLong()
        exitAnimation(provider, exitDurationMs).apply {
            doOnEnd { provider.remove() }
            start()
        }
    }
}

/**
 * The icon first settles back a little (a third of the time), then grows and fades out on the
 * Material emphasized-accelerate curve; the background starts fading once the icon is on its way.
 */
private fun exitAnimation(provider: SplashScreenViewProvider, durationMs: Long): Animator {
    val icon = provider.iconView
    val dipMs = durationMs / DIP_FRACTION
    val dip = AnimatorSet().apply {
        playTogether(icon.scaleTo(DipIconScale), icon.scaleYTo(DipIconScale))
        duration = dipMs
        interpolator = EmphasizedDecelerate
    }
    val zoom = AnimatorSet().apply {
        playTogether(
            icon.scaleTo(ExitIconScale),
            icon.scaleYTo(ExitIconScale),
            ObjectAnimator.ofFloat(icon, View.ALPHA, 0f),
        )
        duration = durationMs - dipMs
        interpolator = EmphasizedAccelerate
    }
    val background = ObjectAnimator.ofFloat(provider.view, View.ALPHA, 0f).apply {
        startDelay = dipMs + (durationMs - dipMs) / 2
        duration = durationMs - startDelay
        interpolator = EmphasizedAccelerate
    }
    return AnimatorSet().apply {
        play(dip).before(zoom)
        play(background).with(dip)
    }
}

/** False when the user turned animations off ("Remove animations" sets the animator scale to 0). */
private fun Activity.animationsEnabled(): Boolean =
    Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f

private fun View.scaleTo(scale: Float) = ObjectAnimator.ofFloat(this, View.SCALE_X, scale)
private fun View.scaleYTo(scale: Float) = ObjectAnimator.ofFloat(this, View.SCALE_Y, scale)

/** Material 3 emphasized easing, matching the in-app motion (see ui/theme/Motion). */
private val EmphasizedAccelerate = PathInterpolator(0.3f, 0f, 0.8f, 0.15f)
private val EmphasizedDecelerate = PathInterpolator(0.05f, 0.7f, 0.1f, 1f)

private const val DIP_FRACTION = 3
private const val DipIconScale = 0.9f
private const val ExitIconScale = 1.8f
