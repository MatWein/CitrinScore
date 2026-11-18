package de.citrinscore.score.musicxml

import com.sun.jna.FunctionMapper
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import de.citrinscore.util.loadNativeLibrary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private const val LIBRARY_NAME = "verovio"
private const val TOOLKIT_PREFIX = "vrvToolkit_"

internal interface VerovioLibrary : Library {
    fun constructorResourcePath(resourcePath: String): Pointer

    fun destructor(toolkit: Pointer)

    fun loadData(toolkit: Pointer, data: String): Boolean

    fun setOptions(toolkit: Pointer, options: String): Boolean

    fun redoLayout(toolkit: Pointer, options: String)

    fun getPageCount(toolkit: Pointer): Int

    fun renderToSVG(toolkit: Pointer, page: Int, xmlDeclaration: Boolean): String

    fun renderToTimemap(toolkit: Pointer, options: String): String

    fun renderToMIDI(toolkit: Pointer): String

    fun getNotatedIdForElement(toolkit: Pointer, id: String): String

    fun getMIDIValuesForElement(toolkit: Pointer, id: String): String

    fun getLog(toolkit: Pointer): String
}

private interface VerovioLogging : Library {
    fun enableLogToBuffer(value: Boolean)
}

internal object Verovio {
    private val lock = Mutex()

    private val library: VerovioLibrary by lazy {
        loadNativeLibrary(LIBRARY_NAME) {
            useLocaleIndependentNumbers()
            Native.load(LIBRARY_NAME, VerovioLogging::class.java).enableLogToBuffer(true)
            Native.load(
                LIBRARY_NAME,
                VerovioLibrary::class.java,
                mapOf(
                    Library.OPTION_STRING_ENCODING to Charsets.UTF_8.name(),
                    Library.OPTION_FUNCTION_MAPPER to FunctionMapper { _, method -> TOOLKIT_PREFIX + method.name },
                ),
            )
        }
    }

    suspend fun <T> call(block: VerovioLibrary.() -> T): T = lock.withLock { withContext(Dispatchers.Default) { library.block() } }

    fun release(toolkit: Pointer) = library.destructor(toolkit)
}
