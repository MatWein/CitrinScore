package de.citrinscore.score

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import de.citrinscore.score.playback.Instrument
import de.citrinscore.score.scan.EraserStroke
import kotlin.time.Instant

@Serializable
data class ScoreManifest(
    val metadata: ScoreMetadata,
    val parts: List<ScorePart>,
    val createdAt: Instant,
    val lastOpenedAt: Instant? = null,
    val currentPart: String? = null,
    val audio: List<Attachment> = emptyList(),
    val files: List<Attachment> = emptyList(),
    val formatVersion: Int = CURRENT_FORMAT_VERSION,
) {
    val lastActivity get() = lastOpenedAt ?: createdAt

    fun attachments(kind: AttachmentKind) = when (kind) {
        AttachmentKind.Audio -> audio
        AttachmentKind.File -> files
    }

    fun withAttachments(kind: AttachmentKind, attachments: List<Attachment>) = when (kind) {
        AttachmentKind.Audio -> copy(audio = attachments)
        AttachmentKind.File -> copy(files = attachments)
    }

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
    val recognition: Recognition? = null,
)

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
    ;

    fun path(fileName: String) = "$directory/$fileName"
}

@Serializable
data class ScoreViewState(val zoom: Float, val x: Float, val y: Float)

@Serializable
data class PlaybackPreferences(val tempo: Float = 1f, val instrument: Instrument? = null)

@Serializable
sealed interface ScoreContent {
    @Serializable
    @SerialName("pages")
    data class Pages(val pages: List<ScorePage>) : ScoreContent

    @Serializable
    @SerialName("musicxml")
    data class MusicXml(val path: String) : ScoreContent
}

@Serializable
data class ScorePage(val source: String, val page: Int = 0, val crop: PageCrop, val normalization: Normalization? = null) {
    val shown get() = normalization?.image ?: source

    val shownPage get() = if (normalization == null) page else 0
}

@Serializable
data class Normalization(val image: String, val threshold: Float, val erased: List<EraserStroke> = emptyList())

val ScoreContent.pageCount: Int?
    get() = when (this) {
        is ScoreContent.Pages -> pages.size
        is ScoreContent.MusicXml -> null
    }

val ScoreContent.isReflowable get() = this is ScoreContent.MusicXml

@Serializable
data class PageCrop(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val size get() = Size(right - left, bottom - top)

    val rect get() = Rect(left, top, right, bottom)
}
