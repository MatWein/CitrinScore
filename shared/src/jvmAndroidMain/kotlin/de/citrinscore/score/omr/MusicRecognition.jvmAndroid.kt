package de.citrinscore.score.omr

import androidx.compose.ui.graphics.ImageBitmap
import de.citrinscore.omr.Omr
import de.citrinscore.omr.OmrImage
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.filesDir
import io.github.vinceglb.filekit.path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.io.File
import kotlin.time.measureTimedValue

private const val MODEL_DIRECTORY = "omr"
private val recognition = Mutex()
private val logger = LoggerFactory.getLogger("de.citrinscore.score.omr.MusicRecognition")

actual suspend fun recognizeMusicXml(pages: List<ImageBitmap>, onProgress: (Float) -> Unit): String = recognition.withLock {
    withContext(Dispatchers.Default) {
        val context = coroutineContext
        val images = pages.map { page -> OmrImage(page.width, page.height, IntArray(page.width * page.height).also { page.readPixels(it) }) }
        val (musicXml, duration) = measureTimedValue {
            Omr(File(FileKit.filesDir.path, MODEL_DIRECTORY)).use { omr ->
                omr.recognize(images) { progress ->
                    context.ensureActive()
                    onProgress(progress)
                }
            }
        }
        logger.info("Recognized {} pages in {}", pages.size, duration)
        musicXml
    }
}
