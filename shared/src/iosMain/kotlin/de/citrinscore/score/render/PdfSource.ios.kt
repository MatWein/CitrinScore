package de.citrinscore.score.render

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.IntSize
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.readBytes
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import platform.CoreFoundation.CFDataCreate
import platform.CoreFoundation.CFRelease
import platform.CoreGraphics.CGBitmapContextCreate
import platform.CoreGraphics.CGBitmapContextGetData
import platform.CoreGraphics.CGColorSpaceCreateDeviceRGB
import platform.CoreGraphics.CGColorSpaceRelease
import platform.CoreGraphics.CGContextConcatCTM
import platform.CoreGraphics.CGContextDrawPDFPage
import platform.CoreGraphics.CGContextFillRect
import platform.CoreGraphics.CGContextRelease
import platform.CoreGraphics.CGContextScaleCTM
import platform.CoreGraphics.CGContextSetInterpolationQuality
import platform.CoreGraphics.CGContextSetRGBFillColor
import platform.CoreGraphics.CGContextTranslateCTM
import platform.CoreGraphics.CGDataProviderCreateWithCFData
import platform.CoreGraphics.CGDataProviderRelease
import platform.CoreGraphics.CGImageAlphaInfo
import platform.CoreGraphics.CGPDFDocumentCreateWithProvider
import platform.CoreGraphics.CGPDFDocumentGetNumberOfPages
import platform.CoreGraphics.CGPDFDocumentGetPage
import platform.CoreGraphics.CGPDFDocumentRef
import platform.CoreGraphics.CGPDFDocumentRelease
import platform.CoreGraphics.CGPDFPageGetBoxRect
import platform.CoreGraphics.CGPDFPageGetDrawingTransform
import platform.CoreGraphics.CGPDFPageGetRotationAngle
import platform.CoreGraphics.CGPDFPageRef
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.kCGInterpolationHigh
import platform.CoreGraphics.kCGPDFCropBox

@OptIn(ExperimentalForeignApi::class)
actual suspend fun openPdfSource(file: PlatformFile): PageSource {
    val bytes = file.readBytes()
    return withContext(Dispatchers.Default) {
        val data = bytes.usePinned { CFDataCreate(null, it.addressOf(0).reinterpret(), bytes.size.convert()) }
        val provider = CGDataProviderCreateWithCFData(data)
        val document = CGPDFDocumentCreateWithProvider(provider)
        CGDataProviderRelease(provider)
        CFRelease(data)
        CoreGraphicsPdfSource(document ?: throw IOException("Unsupported PDF"))
    }
}

@OptIn(ExperimentalForeignApi::class)
private class CoreGraphicsPdfSource(private val document: CGPDFDocumentRef) : PageSource {
    override val pageSizes = (0 until CGPDFDocumentGetNumberOfPages(document).toInt()).map { orientedSize(page(it)) }

    override fun render(page: Int, region: Rect, size: IntSize): ImageBitmap {
        val pdfPage = page(page)
        val pageSize = pageSizes[page]
        val colorSpace = CGColorSpaceCreateDeviceRGB()
        val context = CGBitmapContextCreate(
            null,
            size.width.convert(),
            size.height.convert(),
            8u,
            (size.width * 4).convert(),
            colorSpace,
            CGImageAlphaInfo.kCGImageAlphaPremultipliedLast.value,
        )
        CGColorSpaceRelease(colorSpace)
        try {
            CGContextSetRGBFillColor(context, 1.0, 1.0, 1.0, 1.0)
            CGContextFillRect(context, CGRectMake(0.0, 0.0, size.width.toDouble(), size.height.toDouble()))
            CGContextSetInterpolationQuality(context, kCGInterpolationHigh)
            val scaleX = (size.width / region.width).toDouble()
            val scaleY = (size.height / region.height).toDouble()
            CGContextTranslateCTM(context, -scaleX * region.left, size.height - (pageSize.height - region.top) * scaleY)
            CGContextScaleCTM(context, scaleX, scaleY)
            val pageBounds = CGRectMake(0.0, 0.0, pageSize.width.toDouble(), pageSize.height.toDouble())
            CGContextConcatCTM(context, CGPDFPageGetDrawingTransform(pdfPage, kCGPDFCropBox, pageBounds, 0, false))
            CGContextDrawPDFPage(context, pdfPage)
            val pixels = requireNotNull(CGBitmapContextGetData(context)).reinterpret<ByteVar>().readBytes(size.width * size.height * 4)
            val info = ImageInfo(size.width, size.height, ColorType.RGBA_8888, ColorAlphaType.PREMUL)
            return Image.makeRaster(info, pixels, size.width * 4).toComposeImageBitmap()
        } finally {
            CGContextRelease(context)
        }
    }

    override fun close() = CGPDFDocumentRelease(document)

    private fun page(index: Int): CGPDFPageRef? = CGPDFDocumentGetPage(document, (index + 1).convert())

    private fun orientedSize(page: CGPDFPageRef?): Size = CGPDFPageGetBoxRect(page, kCGPDFCropBox).useContents {
        if (CGPDFPageGetRotationAngle(page) % 180 == 0) {
            Size(size.width.toFloat(), size.height.toFloat())
        } else {
            Size(size.height.toFloat(), size.width.toFloat())
        }
    }
}
