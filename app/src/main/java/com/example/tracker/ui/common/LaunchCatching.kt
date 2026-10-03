package com.example.tracker.ui.common

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Launches [block] and hands any failure to [onError], so a failed user action becomes an
 * error message instead of a crash. Cancellation is never treated as a failure.
 */
fun CoroutineScope.launchCatching(
    onError: suspend (Exception) -> Unit,
    block: suspend CoroutineScope.() -> Unit,
): Job = launch {
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        onError(e)
    }
}
