package de.citrinscore.util

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@OptIn(ExperimentalAtomicApi::class)
class GuardedResource<T>(private val resource: T, private val release: (T) -> Unit) : AutoCloseable {
    private val mutex = Mutex()
    private val closed = AtomicBoolean(false)
    private var released = false

    suspend fun <R> use(block: suspend (T) -> R): R = try {
        mutex.withLock {
            check(!closed.load()) { "Resource is closed" }
            block(resource)
        }
    } finally {
        if (closed.load()) releaseIfIdle()
    }

    override fun close() {
        closed.store(true)
        releaseIfIdle()
    }

    private fun releaseIfIdle() {
        if (!mutex.tryLock()) return
        try {
            if (!released) {
                released = true
                release(resource)
            }
        } finally {
            mutex.unlock()
        }
    }
}
