package de.citrinscore.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory

@PublishedApi
internal val nativeLogger: Logger = LoggerFactory.getLogger("de.citrinscore.util.NativeLibraries")

internal inline fun <T> loadNativeLibrary(name: String, load: () -> T): T {
    nativeLogger.info("Loading native library {}", name)
    return try {
        load().also { nativeLogger.info("Loaded native library {}", name) }
    } catch (e: LinkageError) {
        throw IllegalStateException("Loading native library $name failed", e)
    }
}
