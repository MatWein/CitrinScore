package de.citrinscore.score.render

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntSize
import de.citrinscore.util.GuardedResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PageSourceRenderer(source: PageSource) : ScoreRenderer {
    override val pageSizes = source.pageSizes

    private val source = GuardedResource(source, PageSource::close)

    override suspend fun render(page: Int, region: Rect, size: IntSize): ImageBitmap =
        source.use { withContext(Dispatchers.Default) { it.render(page, region, size) } }

    override fun close() = source.close()
}
