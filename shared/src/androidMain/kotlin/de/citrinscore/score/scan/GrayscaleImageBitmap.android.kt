package de.citrinscore.score.scan

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

internal actual fun grayscaleImageBitmap(width: Int, height: Int, luminance: ByteArray): ImageBitmap {
    val pixels = IntArray(luminance.size) { index -> (luminance[index].toInt() and 0xFF).let { Color.rgb(it, it, it) } }
    return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888).asImageBitmap()
}
