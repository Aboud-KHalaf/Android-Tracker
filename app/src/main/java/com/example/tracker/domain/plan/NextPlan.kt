package com.example.tracker.domain.plan

import com.example.tracker.domain.model.WorkoutPlan

/**
 * The plan to suggest next: a plan never done yet, otherwise the one done longest ago.
 * Plans without exercises can't be started, so they are skipped. Null if none qualify.
 */
fun suggestNextPlan(plans: List<WorkoutPlan>): WorkoutPlan? =
    plans.filter { it.exerciseCount > 0 }
        .minWithOrNull(compareBy(nullsFirst()) { it.lastDoneAt })
