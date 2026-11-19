package de.citrinscore.score.omr

import de.citrinscore.omr.Omr
import de.citrinscore.omr.OmrBox
import de.citrinscore.omr.OmrImage
import de.citrinscore.omr.OmrResult
import de.citrinscore.score.RecognitionLayout
import de.citrinscore.score.RecognizedMeasure
import de.citrinscore.score.RecognizedMusic
import de.citrinscore.score.RecognizedNote
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

actual suspend fun recognizeMusic(pages: List<RecognitionPage>, onProgress: (Float) -> Unit): RecognizedMusic = recognition.withLock {
    withContext(Dispatchers.Default) {
        val context = coroutineContext
        val images = pages.map { page ->
            val image = page.image
            OmrImage(image.width, image.height, IntArray(image.width * image.height).also { image.readPixels(it) })
        }
        val (result, duration) = measureTimedValue {
            Omr(File(FileKit.filesDir.path, MODEL_DIRECTORY)).use { omr ->
                omr.recognize(images) { progress ->
                    context.ensureActive()
                    onProgress(progress)
                }
            }
        }
        logger.info("Recognized {} notes on {} pages in {}", result.notes.size, pages.size, duration)
        result.toMusic(pages.map { it.pixelsPerUnit })
    }
}

private fun OmrResult.toMusic(scales: List<Float>): RecognizedMusic {
    fun OmrBox.scaled(page: Int) = scales[page].let { scale -> listOf(left / scale, top / scale, right / scale, bottom / scale) }
    val layout = RecognitionLayout(
        notes = notes.map { note ->
            val (left, top, right, bottom) = note.bounds.scaled(note.page)
            RecognizedNote(note.id, note.page, note.measure, left, top, right, bottom)
        },
        measures = measures.map { measure ->
            val (left, top, right, bottom) = measure.bounds.scaled(measure.page)
            RecognizedMeasure(measure.page, measure.measure, left, top, right, bottom, measure.space / scales[measure.page])
        },
    )
    return RecognizedMusic(musicXml, layout)
}
