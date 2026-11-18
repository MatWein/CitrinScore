package de.citrinscore.score

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import de.citrinscore.score.playback.Instrument
import kotlin.time.Instant

@Serializable
data class ScoreManifest(
    val metadata: ScoreMetadata,
    val parts: List<ScorePart>,
    val createdAt: Instant,
    val lastOpenedAt: Instant? = null,
    val currentPart: String? = null,
    val formatVersion: Int = CURRENT_FORMAT_VERSION,
) {
    val lastActivity get() = lastOpenedAt ?: createdAt

    fun part(directory: String) = parts.first { it.directory == directory }

    fun updatePart(directory: String, transform: (ScorePart) -> ScorePart) =
        copy(parts = parts.map { if (it.directory == directory) transform(it) else it })

    companion object {
        const val CURRENT_FORMAT_VERSION = 1
    }
}

@Serializable
data class ScorePart(
    val directory: String,
    val name: String,
    val content: ScoreContent,
    val view: ScoreViewState? = null,
    val playback: PlaybackPreferences? = null,
    val systemBreaks: List<List<Float>>? = null,
    val audio: List<Attachment> = emptyList(),
    val files: List<Attachment> = emptyList(),
) {
    fun attachments(kind: AttachmentKind) = when (kind) {
        AttachmentKind.Audio -> audio
        AttachmentKind.File -> files
    }

    fun withAttachments(kind: AttachmentKind, attachments: List<Attachment>) = when (kind) {
        AttachmentKind.Audio -> copy(audio = attachments)
        AttachmentKind.File -> copy(files = attachments)
    }
}

@Serializable
data class ScoreMetadata(
    val title: String,
    val composer: String = "",
    val description: String = "",
)

@Serializable
data class Attachment(val path: String, val name: String) {
    val extension get() = path.substringAfterLast(EXTENSION_SEPARATOR, "")

    val baseName get() = name.takeUnless { extension.isNotEmpty() && it.endsWith("$EXTENSION_SEPARATOR$extension", ignoreCase = true) }
        ?: name.dropLast(extension.length + 1)
}

private const val EXTENSION_SEPARATOR = '.'

enum class AttachmentKind(val directory: String) {
    Audio("audio"),
    File("files"),
}

@Serializable
data class ScoreViewState(val zoom: Float, val x: Float, val y: Float)

@Serializable
data class PlaybackPreferences(val tempo: Float = 1f, val instrument: Instrument? = null)

@Serializable
sealed interface ScoreContent {
    @Serializable
    @SerialName("images")
    data class Images(val pages: List<@Serializable(with = ImagePageSerializer::class) ImagePage>) : ScoreContent

    @Serializable
    @SerialName("pdf")
    data class Pdf(val path: String, @Serializable(with = PdfPagesSerializer::class) val pages: List<PdfPage>) : ScoreContent

    @Serializable
    @SerialName("musicxml")
    data class MusicXml(val path: String) : ScoreContent
}

val ScoreContent.pageCount: Int?
    get() = when (this) {
        is ScoreContent.Images -> pages.size
        is ScoreContent.Pdf -> pages.size
        is ScoreContent.MusicXml -> null
    }

val ScoreContent.isReflowable get() = this is ScoreContent.MusicXml

@Serializable
data class PdfPage(val index: Int, val crop: PageCrop)

@Serializable
data class ImagePage(val path: String, val crop: PageCrop? = null)

@Serializable
data class PageCrop(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val size get() = Size(right - left, bottom - top)

    val rect get() = Rect(left, top, right, bottom)
}

private object PdfPagesSerializer : JsonTransformingSerializer<List<PdfPage>>(ListSerializer(PdfPage.serializer())) {
    override fun transformDeserialize(element: JsonElement): JsonElement = JsonArray(
        element.jsonArray.mapIndexed { index, page ->
            if (PdfPage::crop.name in page.jsonObject) {
                page
            } else {
                buildJsonObject {
                    put(PdfPage::index.name, index)
                    put(PdfPage::crop.name, page)
                }
            }
        },
    )
}

private object ImagePageSerializer : JsonTransformingSerializer<ImagePage>(ImagePage.serializer()) {
    override fun transformDeserialize(element: JsonElement): JsonElement =
        if (element is JsonPrimitive) buildJsonObject { put("path", element) } else element
}

object ScoreManifestSerializer : JsonTransformingSerializer<ScoreManifest>(ScoreManifest.serializer()) {
    private val singlePartKeys = listOf(
        ScorePart::content.name,
        ScorePart::view.name,
        ScorePart::systemBreaks.name,
        ScorePart::audio.name,
        ScorePart::files.name,
    )

    override fun transformDeserialize(element: JsonElement): JsonElement {
        val manifest = element.jsonObject
        if (ScoreManifest::parts.name in manifest) return manifest
        val part = buildJsonObject {
            put(ScorePart::directory.name, "")
            put(ScorePart::name.name, "")
            singlePartKeys.forEach { key -> manifest[key]?.let { put(key, it) } }
        }
        return JsonObject(manifest - singlePartKeys.toSet() + (ScoreManifest::parts.name to JsonArray(listOf(part))))
    }
}
