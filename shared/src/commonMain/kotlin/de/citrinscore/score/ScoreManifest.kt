package de.citrinscore.score

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ScoreManifest(
    val metadata: ScoreMetadata,
    val content: ScoreContent,
    val formatVersion: Int = CURRENT_FORMAT_VERSION,
) {
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
sealed interface ScoreContent {
    @Serializable
    @SerialName("images")
    data class Images(val pages: List<String>) : ScoreContent
}
