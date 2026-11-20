package de.citrinscore.score.render

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntSize
import de.citrinscore.score.PageCrop
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private const val OPEN_SOURCE_LIMIT = 3

class SourcePage(val file: PlatformFile, val index: Int, val crop: PageCrop, val unitWidth: Float? = null)

class SourceScoreRenderer(
    private val pages: List<SourcePage>,
    private val open: suspend (PlatformFile) -> PageSource,
) : ScoreRenderer {
    override val pageSizes = pages.map { it.crop.size }

    private val mutex = Mutex()
    private val sources = LinkedHashMap<String, PageSource>()

    override suspend fun render(page: Int, region: Rect, size: IntSize): ImageBitmap = mutex.withLock {
        val target = pages[page]
        val source = sources.remove(target.file.path) ?: open(target.file)
        sources[target.file.path] = source
        if (sources.size > OPEN_SOURCE_LIMIT) sources.remove(sources.keys.first())?.close()
        val scale = target.unitWidth?.let { source.pageSizes[target.index].width / it } ?: 1f
        withContext(Dispatchers.Default) {
            source.render(target.index, region.translate(target.crop.left, target.crop.top).scaled(scale), size)
        }
    }

    override fun close() {
        sources.values.forEach(PageSource::close)
        sources.clear()
    }
}

fun Rect.scaled(scale: Float) = Rect(left * scale, top * scale, right * scale, bottom * scale)
