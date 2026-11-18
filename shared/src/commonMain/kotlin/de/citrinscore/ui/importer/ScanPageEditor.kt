package de.citrinscore.ui.importer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import de.citrinscore.score.PageCrop
import de.citrinscore.score.scan.EraserStroke
import kotlin.math.abs
import kotlin.math.roundToInt

private val HandleTouchRadius = 24.dp
private val HandleRadius = 6.dp
private val CropBorderWidth = 2.dp
private const val SCRIM_ALPHA = 0.45f
private const val MIN_CROP_FRACTION = 0.05f

@Composable
fun ScanPageEditor(
    scan: ScanPreview,
    tool: ScanTool,
    eraserWidth: Float,
    onCrop: (PageCrop) -> Unit,
    onErase: (EraserStroke) -> Unit,
) {
    val currentScan by rememberUpdatedState(scan)
    val currentOnCrop by rememberUpdatedState(onCrop)
    val currentOnErase by rememberUpdatedState(onErase)
    var stroke by remember { mutableStateOf<EraserStroke?>(null) }
    val accent = MaterialTheme.colorScheme.primary
    Canvas(
        Modifier
            .fillMaxWidth()
            .aspectRatio(scan.size.width / scan.size.height)
            .pointerInput(tool, eraserWidth) {
                when (tool) {
                    ScanTool.Crop -> dragCrop({ currentScan }, HandleTouchRadius.toPx()) { currentOnCrop(it) }
                    ScanTool.Eraser -> erase({ currentScan }, eraserWidth, onStroke = { stroke = it }) { currentOnErase(it) }
                }
            },
    ) {
        val scale = size.width / scan.size.width
        drawImage(
            scan.image,
            dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
            filterQuality = FilterQuality.Medium,
        )
        (scan.erased + listOfNotNull(stroke)).forEach { erased ->
            drawPath(erased.path(scale), Color.White, style = Stroke(erased.width * scale, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        drawCrop(scan.crop.rect.scaled(scale), accent)
    }
}

private fun DrawScope.drawCrop(crop: Rect, accent: Color) {
    val outside = Path().apply {
        fillType = PathFillType.EvenOdd
        addRect(Rect(Offset.Zero, size))
        addRect(crop)
    }
    drawPath(outside, Color.Black.copy(alpha = SCRIM_ALPHA))
    drawRect(accent, crop.topLeft, crop.size, style = Stroke(CropBorderWidth.toPx()))
    listOf(crop.topLeft, crop.topCenter, crop.topRight, crop.centerLeft, crop.centerRight, crop.bottomLeft, crop.bottomCenter, crop.bottomRight)
        .forEach { drawCircle(accent, HandleRadius.toPx(), it) }
}

private suspend fun PointerInputScope.dragCrop(scan: () -> ScanPreview, touchRadius: Float, onCrop: (PageCrop) -> Unit) = awaitEachGesture {
    val down = awaitFirstDown()
    val page = scan()
    val scale = size.width / page.size.width
    val handle = CropHandle.at(page.crop.rect.scaled(scale), down.position, touchRadius) ?: return@awaitEachGesture
    down.consume()
    var crop = page.crop
    drag(down.id) { change ->
        crop = handle.move(crop, change.positionChange() / scale, page.size)
        onCrop(crop)
        change.consume()
    }
}

private suspend fun PointerInputScope.erase(
    scan: () -> ScanPreview,
    eraserWidth: Float,
    onStroke: (EraserStroke?) -> Unit,
    onErase: (EraserStroke) -> Unit,
) = awaitEachGesture {
    val down = awaitFirstDown()
    val page = scan()
    val scale = size.width / page.size.width
    val width = eraserWidth * page.size.width
    down.consume()
    var points = listOf(down.position / scale)
    onStroke(EraserStroke(points, width))
    val completed = drag(down.id) { change ->
        points = points + change.position / scale
        onStroke(EraserStroke(points, width))
        change.consume()
    }
    if (completed) onErase(EraserStroke(points, width))
    onStroke(null)
}

private data class CropHandle(val left: Boolean, val top: Boolean, val right: Boolean, val bottom: Boolean) {

    fun move(crop: PageCrop, delta: Offset, page: Size): PageCrop {
        val minimum = page.width * MIN_CROP_FRACTION
        return PageCrop(
            left = if (left) (crop.left + delta.x).coerceIn(0f, crop.right - minimum) else crop.left,
            top = if (top) (crop.top + delta.y).coerceIn(0f, crop.bottom - minimum) else crop.top,
            right = if (right) (crop.right + delta.x).coerceIn(crop.left + minimum, page.width) else crop.right,
            bottom = if (bottom) (crop.bottom + delta.y).coerceIn(crop.top + minimum, page.height) else crop.bottom,
        )
    }

    companion object {
        fun at(crop: Rect, point: Offset, radius: Float): CropHandle? {
            val inRows = point.y in crop.top - radius..crop.bottom + radius
            val inColumns = point.x in crop.left - radius..crop.right + radius
            val left = inRows && isNearer(point.x, crop.left, crop.right, radius)
            val right = inRows && !left && abs(point.x - crop.right) <= radius
            val top = inColumns && isNearer(point.y, crop.top, crop.bottom, radius)
            val bottom = inColumns && !top && abs(point.y - crop.bottom) <= radius
            return CropHandle(left, top, right, bottom).takeIf { left || top || right || bottom }
        }

        private fun isNearer(value: Float, edge: Float, opposite: Float, radius: Float) =
            abs(value - edge) <= radius && abs(value - edge) <= abs(value - opposite)
    }
}

private fun Rect.scaled(scale: Float) = Rect(left * scale, top * scale, right * scale, bottom * scale)
