package de.citrinscore.omr

import org.w3c.dom.Document
import org.w3c.dom.Element

private fun childTexts(element: Element, names: List<Pair<String, String>>): Map<String, String> =
    names.flatMap { (tag, key) -> element.children(tag).map { key to it.textContent } }.toMap()

private fun clefAttributes(clef: Element) = childTexts(clef, listOf("sign" to "Sign", "sign" to "Line", "clef-octave-change" to "ClefOctaveChange"))

private fun timeAttributes(time: Element) =
    childTexts(time, listOf("beats" to "Beats", "beat-type" to "BeatType", "interchangeable" to "Interchangeable", "senza-misura" to "SenzaMisura"))

private fun keyAttributes(key: Element) = childTexts(key, listOf("fifths" to "Fifths", "key-alter" to "KeyAlter", "mode" to "Mode"))

private class AttributesInForce {
    var divisions: String? = null
    var key: Map<String, String>? = null
    var time: Map<String, String>? = null
    var clef: Map<String, String>? = null

    fun update(measure: Element) {
        for (attributes in measure.children("attributes")) {
            attributes.children("divisions").forEach { divisions = it.textContent }
            attributes.children("key").forEach { key = keyAttributes(it) }
            attributes.children("time").forEach { time = timeAttributes(it) }
            attributes.children("clef").forEach { clef = clefAttributes(it) }
        }
    }
}

internal fun mergePages(pages: List<Document>): Document {
    val main = pages.first()
    val mainParts = main.documentElement.children("part")
    val inForce = mainParts.map { part -> AttributesInForce().also { state -> part.children("measure").forEach(state::update) } }
    for (page in pages.drop(1)) {
        mainParts.forEachIndexed { index, mainPart ->
            val part = page.documentElement.children("part").getOrNull(index) ?: return@forEachIndexed
            val currentLength = mainPart.children("measure").size
            for (measure in part.children("measure")) {
                val number = measure.getAttribute("number").toInt()
                if (number == 1) {
                    val state = inForce[index]
                    for (attributes in measure.children("attributes")) {
                        attributes.children("divisions").filter { it.textContent == state.divisions }.forEach { attributes.removeChild(it) }
                        attributes.children("key").filter { keyAttributes(it) == state.key }.forEach { attributes.removeChild(it) }
                        attributes.children("time").filter { timeAttributes(it) == state.time }.forEach { attributes.removeChild(it) }
                        attributes.children("clef").filter { clefAttributes(it) == state.clef }.forEach { attributes.removeChild(it) }
                    }
                }
                val adopted = main.importNode(measure, true) as Element
                adopted.setAttribute("number", (number + currentLength).toString())
                mainPart.appendChild(adopted)
                inForce[index].update(adopted)
            }
        }
    }
    return main
}
