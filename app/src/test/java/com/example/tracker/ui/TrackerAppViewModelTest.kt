package com.example.tracker.ui

import com.example.tracker.domain.model.Workout
import com.example.tracker.testing.FakeWorkoutRepository
import com.example.tracker.testing.MainDispatcherRule
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TrackerAppViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val workouts = FakeWorkoutRepository().apply {
        activeWorkout.value = Workout("w1", "push", "Push", Instant.parse("2026-10-03T08:30:00Z"), null, emptyList())
    }

    @Test
    fun freshLaunch_reopensTheWorkoutInProgress() = runTest {
        val viewModel = TrackerAppViewModel(workouts)

        viewModel.onFreshStart(requestedWorkoutId = null, restoreActiveWorkout = true)

        assertEquals("w1", viewModel.workoutToOpen.value)
        viewModel.onWorkoutOpened()
        assertNull(viewModel.workoutToOpen.value)
    }

    @Test
    fun freshLaunch_withoutWorkoutInProgress_staysOnHome() = runTest {
        workouts.activeWorkout.value = null
        val viewModel = TrackerAppViewModel(workouts)

        viewModel.onFreshStart(requestedWorkoutId = null, restoreActiveWorkout = true)

        assertNull(viewModel.workoutToOpen.value)
    }

    @Test
    fun otherEntryPoints_leaveNavigationToThem() = runTest {
        val viewModel = TrackerAppViewModel(workouts)

        // E.g. the nutrition reminder's deep link.
        viewModel.onFreshStart(requestedWorkoutId = null, restoreActiveWorkout = false)

        assertNull(viewModel.workoutToOpen.value)
    }

    @Test
    fun notificationTap_opensItsWorkout() = runTest {
        val viewModel = TrackerAppViewModel(workouts)

        viewModel.onFreshStart(requestedWorkoutId = "w1", restoreActiveWorkout = false)
        assertEquals("w1", viewModel.workoutToOpen.value)

        viewModel.onWorkoutOpened()
        viewModel.onOpenWorkout("w1")
        assertEquals("w1", viewModel.workoutToOpen.value)
    }
}
