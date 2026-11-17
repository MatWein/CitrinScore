package de.citrinscore.score.musicxml

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import org.w3c.dom.Element
import org.w3c.dom.Node
import javax.xml.parsers.DocumentBuilderFactory

private const val CLASS = "class"
private const val VIEW_BOX = "viewBox"
private const val TRANSFORM = "transform"
private const val DEFINITION_SCALE = "definition-scale"
private const val PAGE_MARGIN = "page-margin"
private const val MEASURE = "measure"
private const val STAFF = "staff"
private const val PATH = "path"
private const val LAYER = "layer"
private const val USE = "use"
private const val DEFAULT_SPACE = 18f
private val HorizontalLine = Regex("""^M(-?[\d.]+) (-?[\d.]+) L(-?[\d.]+) (-?[\d.]+)$""")
private val Translate = Regex("""translate\((-?[\d.]+),\s*(-?[\d.]+)\)""")
private val Whitespace = Regex("\\s+")

internal fun staffBoxes(svg: String, firstMeasure: Int): List<StaffBox> {
    val root = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(svg.byteInputStream()).documentElement
    val definition = root.descendants().first { it.hasClass(DEFINITION_SCALE) }
    val scale = root.viewBoxWidth() / definition.viewBoxWidth()
    val margin = root.descendants().firstOrNull { it.hasClass(PAGE_MARGIN) }?.translation() ?: Offset.Zero
    return root.descendants().filter { it.hasClass(MEASURE) }.flatMapIndexed { index, measure ->
        measure.children().filter { it.hasClass(STAFF) }.mapIndexedNotNull { staff, element ->
            element.staffBox(firstMeasure + index, staff, margin, scale)
        }.toList()
    }.toList()
}

private fun Element.staffBox(measure: Int, staff: Int, margin: Offset, scale: Float): StaffBox? {
    val lines = children().filter { it.tagName == PATH }.mapNotNull { HorizontalLine.matchEntire(it.getAttribute("d"))?.groupValues }
        .map { values -> values.drop(1).map(String::toFloat) }
        .filter { (_, y1, _, y2) -> y1 == y2 }
        .toList()
    if (lines.isEmpty()) return null
    val top = lines.minOf { it[1] }
    val bottom = lines.maxOf { it[1] }
    val bounds = Rect(lines.minOf { minOf(it[0], it[2]) }, top, lines.maxOf { maxOf(it[0], it[2]) }, bottom)
        .translate(margin).let { Rect(it.left * scale, it.top * scale, it.right * scale, it.bottom * scale) }
    val space = if (lines.size > 1) bounds.height / (lines.size - 1) else DEFAULT_SPACE
    val firstElement = children().filter { it.hasClass(LAYER) }.flatMap { it.descendants() }.filter { it.tagName == USE }
        .mapNotNull { it.translation()?.x }.minOrNull()
    val contentLeft = firstElement?.let { (it + margin.x) * scale }?.takeIf { it < bounds.right } ?: bounds.left
    return StaffBox(measure, staff, bounds, space, contentLeft)
}

private fun Element.hasClass(name: String) = getAttribute(CLASS).split(Whitespace).contains(name)

private fun Element.viewBoxWidth() = getAttribute(VIEW_BOX).trim().split(Whitespace)[2].toFloat()

private fun Element.translation() = Translate.find(getAttribute(TRANSFORM))?.groupValues?.let { Offset(it[1].toFloat(), it[2].toFloat()) }

private fun Element.children(): Sequence<Element> =
    (0 until childNodes.length).asSequence().map(childNodes::item).filter { it.nodeType == Node.ELEMENT_NODE }.map { it as Element }

private fun Element.descendants(): Sequence<Element> = children().flatMap { sequenceOf(it) + it.descendants() }
