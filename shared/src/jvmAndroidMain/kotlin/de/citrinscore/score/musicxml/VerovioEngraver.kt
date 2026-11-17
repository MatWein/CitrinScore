package de.citrinscore.score.musicxml

import com.sun.jna.Pointer
import de.citrinscore.util.GuardedResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val PAGE_MARGIN = 50
private const val CLASS_STYLES = ".placeholder {display:none} .ending, .fing, .reh, .tempo {font-weight:bold} .dir, .dynam, .mNum {font-style:italic} .label {font-weight:normal}"
private const val UTF8_BOM = "﻿"
private val Utf16Boms = listOf(byteArrayOf(0xFE.toByte(), 0xFF.toByte()), byteArrayOf(0xFF.toByte(), 0xFE.toByte()))

actual suspend fun openMusicXmlEngraver(data: ByteArray, compressed: Boolean): MusicXmlEngraver {
    val resources = verovioResourcePath()
    val toolkit = Verovio.call {
        val toolkit = constructorResourcePath(resources)
        setOptions(toolkit, options(EngravingLayout.Print))
        val loaded = if (compressed) loadZipDataBuffer(toolkit, data, data.size) else loadData(toolkit, data.decodeXml())
        val log = getLog(toolkit)
        if (!loaded) {
            destructor(toolkit)
            error("Verovio could not load the score: $log")
        }
        toolkit
    }
    return VerovioEngraver(GuardedResource(toolkit, Verovio::release))
}

private class VerovioEngraver(private val toolkit: GuardedResource<Pointer>) : MusicXmlEngraver {
    override suspend fun engrave(layout: EngravingLayout): List<EngravedPage> {
        val pages = toolkit.use { handle ->
            Verovio.call {
                setOptions(handle, options(layout))
                redoLayout(handle, "{}")
                val pages = (1..getPageCount(handle)).map { page -> renderToSVG(handle, page, false) }
                getLog(handle)
                pages
            }
        }
        return withContext(Dispatchers.Default) {
            var firstMeasure = 0
            pages.map { svg ->
                val staves = staffBoxes(svg, firstMeasure)
                firstMeasure = (staves.maxOfOrNull { it.measure } ?: (firstMeasure - 1)) + 1
                EngravedPage(svg, staves)
            }
        }
    }

    override fun close() = toolkit.close()
}

private fun options(layout: EngravingLayout) = buildJsonObject {
    put("pageWidth", layout.pageWidth)
    put("pageHeight", layout.pageHeight)
    put("pageMarginLeft", PAGE_MARGIN)
    put("pageMarginRight", PAGE_MARGIN)
    put("pageMarginTop", PAGE_MARGIN)
    put("pageMarginBottom", PAGE_MARGIN)
    put("adjustPageHeight", true)
    put("breaks", "auto")
    put("footer", "none")
    put("svgCss", CLASS_STYLES)
    put("svgViewBox", true)
}.toString()

private fun ByteArray.decodeXml(): String =
    if (Utf16Boms.any { bom -> bom.indices.all { getOrNull(it) == bom[it] } }) toString(Charsets.UTF_16) else decodeToString().removePrefix(UTF8_BOM)
