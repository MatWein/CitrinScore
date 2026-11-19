package de.citrinscore.omr

import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.StringWriter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

internal fun newDocument(): Document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()

internal fun Document.serialize(): String = StringWriter().also { writer ->
    TransformerFactory.newInstance().newTransformer().apply {
        setOutputProperty(OutputKeys.ENCODING, "UTF-8")
        setOutputProperty(OutputKeys.STANDALONE, "no")
    }.transform(DOMSource(this), StreamResult(writer))
}.toString()

internal fun Element.element(tag: String, text: String? = null, vararg attributes: Pair<String, String>): Element =
    ownerDocument.createElement(tag).also { element ->
        attributes.forEach { (name, value) -> element.setAttribute(name, value) }
        if (text != null) element.textContent = text
    }

internal fun Element.add(tag: String, text: String? = null, vararg attributes: Pair<String, String>): Element =
    element(tag, text, *attributes).also { appendChild(it) }

internal val Element.elements: List<Element>
    get() {
        val nodes = childNodes
        return (0 until nodes.length).mapNotNull { nodes.item(it) as? Element }
    }

internal fun Element.children(tag: String) = elements.filter { it.tagName == tag }

internal fun Element.findAll(path: String): List<Element> =
    path.split("/").fold(listOf(this)) { current, tag -> current.flatMap { it.children(tag) } }

internal fun Element.find(path: String): Element? = findAll(path).firstOrNull()

internal fun Element.findText(path: String, default: String = ""): String = find(path)?.textContent ?: default

internal fun Element.descendants(tag: String): List<Element> {
    val nodes = getElementsByTagName(tag)
    return (0 until nodes.length).map { nodes.item(it) as Element }
}

internal fun Element.insertAt(index: Int, child: Element) {
    val current = elements
    if (index >= current.size) appendChild(child) else insertBefore(child, current[index])
}

internal fun Element.indexOf(child: Element) = elements.indexOf(child)
