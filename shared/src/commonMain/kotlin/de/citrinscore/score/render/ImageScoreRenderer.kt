package de.citrinscore.score.render

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntSize
import de.citrinscore.score.PageCrop
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private const val OpenSourceLimit = 3

class ImageScoreRenderer(private val files: List<PlatformFile>, private val crops: List<PageCrop>) : ScoreRenderer {
    override val pageSizes = crops.map { it.size }

    private val mutex = Mutex()
    private val sources = LinkedHashMap<Int, ImageSource>()

    override suspend fun render(page: Int, region: Rect, size: IntSize): ImageBitmap = mutex.withLock {
        val source = sources.remove(page) ?: ImageSource(files[page])
        sources[page] = source
        if (sources.size > OpenSourceLimit) sources.remove(sources.keys.first())?.close()
        val crop = crops[page]
        withContext(Dispatchers.Default) { source.render(region.translate(crop.left.toFloat(), crop.top.toFloat()), size) }
    }

    override fun close() {
        sources.values.forEach(ImageSource::close)
        sources.clear()
    }
}
