package de.citrinscore.omr

import ai.onnxruntime.OrtEnvironment
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.slf4j.LoggerFactory
import org.w3c.dom.Document
import java.io.File

private val logger = LoggerFactory.getLogger(Omr::class.java)

class OmrImage(val width: Int, val height: Int, val argb: IntArray) {
    init {
        require(argb.size == width * height) { "Expected ${width * height} pixels, got ${argb.size}" }
    }
}

class OmrException(message: String) : Exception(message)

data class OmrBox(val left: Float, val top: Float, val right: Float, val bottom: Float)

data class OmrNote(val id: String, val page: Int, val measure: Int, val bounds: OmrBox)

data class OmrMeasure(val page: Int, val measure: Int, val bounds: OmrBox, val space: Float)

class OmrResult(val musicXml: String, val notes: List<OmrNote>, val measures: List<OmrMeasure>)

class Omr(modelDirectory: File) : AutoCloseable {
    private val environment = OrtEnvironment.getEnvironment()
    private val segnet: Segnet
    private val relations: StaffRelations
    private val transformer: Transformer

    init {
        loadOpenCv()
        val models = extractModels(modelDirectory)
        segnet = Segnet(environment, models.getValue(OmrModelFiles.SEGNET))
        relations = StaffRelations(environment, models.getValue(OmrModelFiles.STAFF_RELATIONS), models.getValue(OmrModelFiles.STAFF_PIECES))
        transformer = Transformer(environment, models.getValue(OmrModelFiles.ENCODER), models.getValue(OmrModelFiles.DECODER))
    }

    fun recognize(pages: List<OmrImage>, onProgress: (Float) -> Unit = {}): OmrResult {
        val recognizer = PageRecognizer(segnet, relations, transformer)
        val results = mutableMapOf<Int, PageResult>()
        var inheritedTimes: List<TimeSignature?>? = null
        pages.forEachIndexed { index, page ->
            onProgress(index.toFloat() / pages.size)
            val result = page.toBgr().use { image ->
                try {
                    recognizer.recognize(image, inheritedTimes, "p${index + 1}") { done, total -> onProgress((index + done.toFloat() / total) / pages.size) }
                } catch (e: NoStaffsFoundException) {
                    logger.info("Skipping page {}: {}", index + 1, e.message)
                    null
                }
            } ?: return@forEachIndexed
            results[index] = result
            inheritedTimes = lastTimeSignatures(result.document.documentElement)
        }
        onProgress(1f)
        if (results.isEmpty()) throw OmrException("No staffs found on any page")
        val merged = mergePages(results.values.map { it.document })
        val measureOf = measuresOfNotes(merged)
        val notes = results.flatMap { (page, result) ->
            result.notes.mapNotNull { (id, located) -> measureOf[id]?.let { measure -> Triple(OmrNote(id, page, measure, located.first.toBox()), located.second, result) } }
        }
        val measures = notes.groupBy { (note, system, _) -> Triple(note.page, note.measure, system) }.mapNotNull { (key, members) ->
            val system = members.first().third.systems.getOrNull(key.third) ?: return@mapNotNull null
            val space = system.space
            val left = maxOf(system.bounds.left, members.minOf { it.first.bounds.left } - space)
            val right = minOf(system.bounds.right, members.maxOf { it.first.bounds.right } + space)
            OmrMeasure(key.first, key.second, Bounds(left, system.bounds.top, right, system.bounds.bottom).toBox(), space.toFloat())
        }
        return OmrResult(merged.serialize(), notes.map { it.first }, measures)
    }

    private fun measuresOfNotes(document: Document): Map<String, Int> = buildMap {
        document.documentElement.children("part").forEach { part ->
            part.children("measure").forEach { measure ->
                val number = measure.getAttribute("number").toInt()
                measure.children("note").forEach { note -> if (note.hasAttribute("id")) put(note.getAttribute("id"), number) }
            }
        }
    }

    private fun Bounds.toBox() = OmrBox(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())

    override fun close() {
        segnet.close()
        relations.close()
        transformer.close()
    }

    private fun OmrImage.toBgr(): Mat {
        val bgr = ByteArray(width * height * 3)
        argb.forEachIndexed { index, pixel ->
            val alpha = pixel ushr 24
            fun channel(shift: Int) = (((pixel shr shift) and 0xFF) * alpha + 255 * (255 - alpha)) / 255
            bgr[index * 3] = channel(0).toByte()
            bgr[index * 3 + 1] = channel(8).toByte()
            bgr[index * 3 + 2] = channel(16).toByte()
        }
        return byteMat(height, width, bgr, CvType.CV_8UC3)
    }

    private companion object {
        fun extractModels(directory: File): Map<String, String> {
            directory.mkdirs()
            val names = listOf(OmrModelFiles.SEGNET, OmrModelFiles.ENCODER, OmrModelFiles.DECODER, OmrModelFiles.STAFF_RELATIONS, OmrModelFiles.STAFF_PIECES)
            directory.listFiles()?.filter { it.name !in names }?.forEach { it.deleteRecursively() }
            return names.associateWith { name ->
                val target = File(directory, name)
                if (!target.exists()) {
                    logger.info("Extracting OMR model {}", name)
                    val partial = File(directory, "$name.part")
                    val resource = "${OmrModelFiles.DIRECTORY}/$name"
                    val input = Omr::class.java.classLoader?.getResourceAsStream(resource) ?: error("Missing OMR model $resource")
                    input.use { stream -> partial.outputStream().use { stream.copyTo(it) } }
                    check(partial.renameTo(target)) { "Could not store OMR model $name" }
                }
                target.absolutePath
            }
        }
    }
}
