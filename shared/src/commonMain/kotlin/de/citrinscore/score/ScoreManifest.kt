package de.citrinscore.score

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer
import kotlinx.serialization.json.buildJsonObject
import kotlin.time.Instant

@Serializable
data class ScoreManifest(
    val metadata: ScoreMetadata,
    val content: ScoreContent,
    val createdAt: Instant,
    val lastOpenedAt: Instant? = null,
    val view: ScoreViewState? = null,
    val systemBreaks: List<List<Float>>? = null,
    val audio: List<AudioAttachment> = emptyList(),
    val formatVersion: Int = CURRENT_FORMAT_VERSION,
) {
    val lastActivity get() = lastOpenedAt ?: createdAt

    companion object {
        const val CURRENT_FORMAT_VERSION = 1
    }
}

@Serializable
data class ScoreMetadata(
    val title: String,
    val composer: String = "",
    val description: String = "",
)

@Serializable
data class AudioAttachment(val path: String, val name: String)

@Serializable
data class ScoreViewState(val zoom: Float, val x: Float, val y: Float)

@Serializable
sealed interface ScoreContent {
    @Serializable
    @SerialName("images")
    data class Images(val pages: List<@Serializable(with = ImagePageSerializer::class) ImagePage>) : ScoreContent

    @Serializable
    @SerialName("pdf")
    data class Pdf(val path: String, val pages: List<PageCrop>) : ScoreContent
}

@Serializable
data class ImagePage(val path: String, val crop: PageCrop? = null)

@Serializable
data class PageCrop(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val size get() = Size(right - left, bottom - top)

    val rect get() = Rect(left, top, right, bottom)
}

private object ImagePageSerializer : JsonTransformingSerializer<ImagePage>(ImagePage.serializer()) {
    override fun transformDeserialize(element: JsonElement): JsonElement =
        if (element is JsonPrimitive) buildJsonObject { put("path", element) } else element
}
