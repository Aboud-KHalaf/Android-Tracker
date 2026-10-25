package com.example.tracker.domain.stats

import com.example.tracker.domain.model.StatGoal

/**
 * Progress toward [target] from 0 to 1, measured as [goal] says: for [StatGoal.DAILY] the
 * [latest] value against the target, for [StatGoal.CHANGE] how far [latest] has come from
 * [first]. Going past the target counts as done, never more.
 */
fun progressToward(goal: StatGoal, target: Double, first: Double, latest: Double): Float = when (goal) {
    StatGoal.DAILY -> dailyProgress(target, latest)
    StatGoal.CHANGE -> changeProgress(target, first, latest)
}

/** A target of zero or less is reached by any value at or above it. */
private fun dailyProgress(target: Double, latest: Double): Float = when {
    latest >= target -> 1f
    target <= 0.0 || latest <= 0.0 -> 0f
    else -> (latest / target).toFloat()
}

/** Going up or down is read from where [first] started; a first entry on the target counts as done. */
private fun changeProgress(target: Double, first: Double, latest: Double): Float {
    val total = target - first
    if (total == 0.0) return 1f
    return ((latest - first) / total).coerceIn(0.0, 1.0).toFloat()
}
