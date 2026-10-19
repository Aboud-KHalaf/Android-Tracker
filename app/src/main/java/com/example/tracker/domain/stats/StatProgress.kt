package com.example.tracker.domain.stats

/**
 * How far [latest] has come from [first] toward [target], from 0 to 1. Going up or down
 * is read from where [first] started; a first entry already on the target counts as done.
 */
fun progressToward(target: Double, first: Double, latest: Double): Float {
    val total = target - first
    if (total == 0.0) return 1f
    return ((latest - first) / total).coerceIn(0.0, 1.0).toFloat()
}
