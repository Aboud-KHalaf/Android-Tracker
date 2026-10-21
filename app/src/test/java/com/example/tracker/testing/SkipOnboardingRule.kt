package com.example.tracker.testing

import androidx.test.core.app.ApplicationProvider
import com.example.tracker.TrackerApplication
import kotlinx.coroutines.runBlocking
import org.junit.rules.ExternalResource

/**
 * Marks onboarding as done before the activity starts, for app tests that begin on Home. Order
 * it before the compose rule, which launches the activity.
 */
class SkipOnboardingRule : ExternalResource() {
    override fun before() = runBlocking {
        ApplicationProvider.getApplicationContext<TrackerApplication>().container.settingsRepository.setOnboardingDone()
    }
}
