package de.citrinscore.score.render

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntSize
import androidx.exifinterface.media.ExifInterface
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.math.floor
import kotlin.math.max
import android.graphics.Rect as AndroidRect

actual suspend fun openImageSource(file: PlatformFile): PageSource {
    val bytes = file.readBytes()
    return withContext(Dispatchers.Default) {
        val decoder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            BitmapRegionDecoder.newInstance(bytes, 0, bytes.size)
        } else {
            @Suppress("DEPRECATION")
            BitmapRegionDecoder.newInstance(bytes, 0, bytes.size, false)
        } ?: throw IOException("Unsupported image")
        val exif = ExifInterface(bytes.inputStream())
        AndroidImageSource(decoder, exif.rotationDegrees, exif.isFlipped)
    }
}

private class AndroidImageSource(
    private val decoder: BitmapRegionDecoder,
    rotationDegrees: Int,
    flipped: Boolean,
) : PageSource {
    private val rawBounds = RectF(0f, 0f, decoder.width.toFloat(), decoder.height.toFloat())
    private val rawToOriented = Matrix().apply {
        if (flipped) postScale(-1f, 1f)
        postRotate(rotationDegrees.toFloat())
        val oriented = RectF().also { mapRect(it, rawBounds) }
        postTranslate(-oriented.left, -oriented.top)
    }
    private val orientedToRaw = Matrix().also { rawToOriented.invert(it) }

    override val pageSizes = listOf(RectF().also { rawToOriented.mapRect(it, rawBounds) }.let { Size(it.width(), it.height()) })

    override fun render(page: Int, region: Rect, size: IntSize): ImageBitmap {
        val rawRegion = RectF().also { orientedToRaw.mapRect(it, RectF(region.left, region.top, region.right, region.bottom)) }
        val decodeRect = AndroidRect().also { rawRegion.roundOut(it) }
        val output = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888).apply { eraseColor(android.graphics.Color.WHITE) }
        if (!decodeRect.intersect(0, 0, decoder.width, decoder.height)) return output.asImageBitmap()
        val reduction = region.width / size.width
        val options = BitmapFactory.Options().apply {
            inSampleSize = Integer.highestOneBit(max(1, floor(reduction).toInt()))
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = synchronized(decoder) { decoder.decodeRegion(decodeRect, options) }
        val transform = Matrix().apply {
            setScale(decodeRect.width().toFloat() / decoded.width, decodeRect.height().toFloat() / decoded.height)
            postTranslate(decodeRect.left.toFloat(), decodeRect.top.toFloat())
            postConcat(rawToOriented)
            postTranslate(-region.left, -region.top)
            postScale(size.width / region.width, size.height / region.height)
        }
        Canvas(output).drawBitmap(decoded, transform, Paint(Paint.FILTER_BITMAP_FLAG))
        decoded.recycle()
        return output.asImageBitmap()
    }

    override fun close() = decoder.recycle()
}
