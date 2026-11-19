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

    fun recognize(pages: List<OmrImage>, onProgress: (Float) -> Unit = {}): String {
        val recognizer = PageRecognizer(segnet, relations, transformer)
        val documents = mutableListOf<Document>()
        var inheritedTimes: List<TimeSignature?>? = null
        pages.forEachIndexed { index, page ->
            onProgress(index.toFloat() / pages.size)
            val document = page.toBgr().use { image ->
                try {
                    recognizer.recognize(image, inheritedTimes) { done, total -> onProgress((index + done.toFloat() / total) / pages.size) }
                } catch (e: NoStaffsFoundException) {
                    logger.info("Skipping page {}: {}", index + 1, e.message)
                    null
                }
            } ?: return@forEachIndexed
            documents += document
            inheritedTimes = lastTimeSignatures(document.documentElement)
        }
        onProgress(1f)
        if (documents.isEmpty()) throw OmrException("No staffs found on any page")
        return mergePages(documents).serialize()
    }

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
