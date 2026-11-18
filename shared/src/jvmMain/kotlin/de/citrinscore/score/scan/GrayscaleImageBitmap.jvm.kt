package de.citrinscore.score.scan

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo

internal actual fun grayscaleImageBitmap(width: Int, height: Int, luminance: ByteArray): ImageBitmap = Bitmap().apply {
    allocPixels(ImageInfo(width, height, ColorType.GRAY_8, ColorAlphaType.OPAQUE))
    installPixels(luminance)
}.setImmutable().asComposeImageBitmap()
