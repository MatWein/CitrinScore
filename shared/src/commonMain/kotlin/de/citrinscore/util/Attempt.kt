package de.citrinscore.util

import kotlinx.coroutines.CancellationException

inline fun <T> attempt(block: () -> T): T? = try {
    block()
} catch (e: CancellationException) {
    throw e
} catch (_: Exception) {
    null
}
