package de.citrinscore.score.musicxml

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

private const val CLASS = "class"
private const val VIEW_BOX = "viewBox"
private const val TRANSFORM = "transform"
private const val ID = "id"
private const val HREF = "xlink:href"
private const val DEFINITION_SCALE = "definition-scale"
private const val PAGE_MARGIN = "page-margin"
private const val MEASURE = "measure"
private const val STAFF = "staff"
private const val NOTE = "note"
private const val NOTEHEAD = "notehead"
private const val PATH = "path"
private const val LAYER = "layer"
private const val USE = "use"
private const val GLYPH = "g"
private const val GLYPH_CODE = "c"
private const val DEFAULT_SPACE = 18f
private val HorizontalLine = Regex("""^M(-?[\d.]+) (-?[\d.]+) L(-?[\d.]+) (-?[\d.]+)$""")
private val Translate = Regex("""translate\((-?[\d.]+),\s*(-?[\d.]+)\)""")
private val Scale = Regex("""scale\((-?[\d.]+),\s*(-?[\d.]+)\)""")
private val Whitespace = Regex("\\s+")

internal class PageGeometry(val staves: List<StaffBox>, val notes: List<NoteBox>)

internal class GlyphBounds(file: File) {
    private val bounds = parse(file.readBytes()).descendants().filter { it.tagName == GLYPH }.associate { glyph ->
        val (x, y, width, height) = listOf("x", "y", "w", "h").map { glyph.getAttribute(it).toFloat() }
        glyph.getAttribute(GLYPH_CODE) to Rect(x, -(y + height), x + width, -y)
    }

    operator fun get(code: String): Rect? = bounds[code]
}

internal fun pageGeometry(svg: String, firstMeasure: Int, glyphs: GlyphBounds): PageGeometry {
    val root = parse(svg.encodeToByteArray())
    val definition = root.descendants().first { it.hasClass(DEFINITION_SCALE) }
    val page = PageTransform(
        margin = root.descendants().firstOrNull { it.hasClass(PAGE_MARGIN) }?.translation() ?: Offset.Zero,
        scale = root.viewBoxWidth() / definition.viewBoxWidth(),
    )
    val measures = root.descendants().filter { it.hasClass(MEASURE) }.toList()
    val staves = measures.flatMapIndexed { index, measure ->
        measure.children().filter { it.hasClass(STAFF) }.mapIndexedNotNull { staff, element ->
            element.staffBox(firstMeasure + index, staff, page)
        }.toList()
    }
    val notes = measures.flatMapIndexed { index, measure ->
        measure.descendants().filter { it.hasClass(NOTE) }.mapNotNull { it.noteBox(firstMeasure + index, page, glyphs) }.toList()
    }
    return PageGeometry(staves, notes)
}

private class PageTransform(val margin: Offset, val scale: Float) {
    fun apply(rect: Rect) = rect.translate(margin).let { Rect(it.left * scale, it.top * scale, it.right * scale, it.bottom * scale) }

    fun apply(x: Float) = (x + margin.x) * scale
}

private fun Element.staffBox(measure: Int, staff: Int, page: PageTransform): StaffBox? {
    val lines = children().filter { it.tagName == PATH }.mapNotNull { HorizontalLine.matchEntire(it.getAttribute("d"))?.groupValues }
        .map { values -> values.drop(1).map(String::toFloat) }
        .filter { (_, y1, _, y2) -> y1 == y2 }
        .toList()
    if (lines.isEmpty()) return null
    val bounds = page.apply(Rect(lines.minOf { minOf(it[0], it[2]) }, lines.minOf { it[1] }, lines.maxOf { maxOf(it[0], it[2]) }, lines.maxOf { it[1] }))
    val space = if (lines.size > 1) bounds.height / (lines.size - 1) else DEFAULT_SPACE
    val firstElement = children().filter { it.hasClass(LAYER) }.flatMap { it.descendants() }.filter { it.tagName == USE }
        .mapNotNull { it.translation()?.x }.minOrNull()
    val contentLeft = firstElement?.let(page::apply)?.takeIf { it < bounds.right } ?: bounds.left
    return StaffBox(measure, staff, bounds, space, contentLeft)
}

private fun Element.noteBox(measure: Int, page: PageTransform, glyphs: GlyphBounds): NoteBox? {
    val head = children().firstOrNull { it.hasClass(NOTEHEAD) }?.children()?.firstOrNull { it.tagName == USE } ?: return null
    val glyph = glyphs[head.getAttribute(HREF).removePrefix("#").substringBefore("-")] ?: return null
    val origin = head.translation() ?: return null
    val scale = Scale.find(head.getAttribute(TRANSFORM))?.groupValues?.get(1)?.toFloat() ?: 1f
    val bounds = Rect(glyph.left * scale, glyph.top * scale, glyph.right * scale, glyph.bottom * scale).translate(origin)
    return NoteBox(getAttribute(ID), measure, page.apply(bounds))
}

private fun parse(xml: ByteArray): Element = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xml.inputStream()).documentElement

private fun Element.hasClass(name: String) = getAttribute(CLASS).split(Whitespace).contains(name)

private fun Element.viewBoxWidth() = getAttribute(VIEW_BOX).trim().split(Whitespace)[2].toFloat()

private fun Element.translation() = Translate.find(getAttribute(TRANSFORM))?.groupValues?.let { Offset(it[1].toFloat(), it[2].toFloat()) }

private fun Element.children(): Sequence<Element> =
    (0 until childNodes.length).asSequence().map(childNodes::item).filter { it.nodeType == Node.ELEMENT_NODE }.map { it as Element }

private fun Element.descendants(): Sequence<Element> = children().flatMap { sequenceOf(it) + it.descendants() }
