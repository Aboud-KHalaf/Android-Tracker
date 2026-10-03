package com.example.tracker.domain.plan

import com.example.tracker.domain.model.WorkoutPlan
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NextPlanTest {

    private fun plan(id: String, lastDone: String?, exercises: Int = 5) =
        WorkoutPlan(id, id, exercises, lastDone?.let(Instant::parse))

    @Test
    fun suggestNextPlan_picksPlanDoneLongestAgo() {
        val plans = listOf(
            plan("push", "2026-09-28T10:00:00Z"),
            plan("pull", "2026-09-30T10:00:00Z"),
            plan("legs", "2026-10-02T10:00:00Z"),
        )

        assertEquals("push", suggestNextPlan(plans)?.id)
    }

    @Test
    fun suggestNextPlan_prefersPlanNeverDone() {
        val plans = listOf(plan("push", "2026-09-28T10:00:00Z"), plan("new", null))

        assertEquals("new", suggestNextPlan(plans)?.id)
    }

    @Test
    fun suggestNextPlan_skipsPlansWithoutExercises() {
        val plans = listOf(plan("empty", null, exercises = 0), plan("push", "2026-09-28T10:00:00Z"))

        assertEquals("push", suggestNextPlan(plans)?.id)
    }

    @Test
    fun suggestNextPlan_noStartablePlans_returnsNull() {
        assertNull(suggestNextPlan(emptyList()))
        assertNull(suggestNextPlan(listOf(plan("empty", null, exercises = 0))))
    }
}
