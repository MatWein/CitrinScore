package de.citrinscore.score.render

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.batik.anim.dom.SAXSVGDocumentFactory
import org.apache.batik.bridge.BridgeContext
import org.apache.batik.bridge.GVTBuilder
import org.apache.batik.bridge.UserAgentAdapter
import org.apache.batik.util.SVGConstants
import org.apache.batik.util.XMLResourceDescriptor
import java.awt.Color
import java.awt.RenderingHints
import java.awt.image.BufferedImage

actual suspend fun openSvgSource(bytes: ByteArray): PageSource = withContext(Dispatchers.Default) { BatikSvgSource(bytes) }

private class BatikSvgSource(bytes: ByteArray) : PageSource {
    private val context = BridgeContext(UserAgentAdapter())
    private val root = run {
        val document = SAXSVGDocumentFactory(XMLResourceDescriptor.getXMLParserClassName()).createSVGDocument(null, bytes.inputStream())
        val svg = document.rootElement
        if (svg.hasAttribute(SVGConstants.SVG_VIEW_BOX_ATTRIBUTE)) {
            val viewBox = svg.viewBox.baseVal
            svg.setAttribute(SVGConstants.SVG_WIDTH_ATTRIBUTE, viewBox.width.toString())
            svg.setAttribute(SVGConstants.SVG_HEIGHT_ATTRIBUTE, viewBox.height.toString())
        }
        GVTBuilder().build(context, document)
    }

    override val pageSizes = context.documentSize.let { size ->
        check(size.width > 0 && size.height > 0) { "SVG has no size" }
        listOf(Size(size.width.toFloat(), size.height.toFloat()))
    }

    override fun render(page: Int, region: Rect, size: IntSize): ImageBitmap {
        val image = BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics()
        try {
            graphics.color = Color.WHITE
            graphics.fillRect(0, 0, size.width, size.height)
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
            graphics.scale(size.width / region.width.toDouble(), size.height / region.height.toDouble())
            graphics.translate(-region.left.toDouble(), -region.top.toDouble())
            root.paint(graphics)
        } finally {
            graphics.dispose()
        }
        return image.toComposeImageBitmap()
    }

    override fun close() = context.dispose()
}
