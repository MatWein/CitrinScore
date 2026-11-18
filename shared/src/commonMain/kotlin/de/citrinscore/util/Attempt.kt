package de.citrinscore.util

import kotlinx.coroutines.CancellationException
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@PublishedApi
internal val attemptLogger: Logger = LoggerFactory.getLogger("de.citrinscore.Attempt")

inline fun <T> attempt(block: () -> T): T? = try {
    block()
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    attemptLogger.warn("Operation failed", e)
    null
}
