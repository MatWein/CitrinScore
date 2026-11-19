package de.citrinscore.ui.importer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import de.citrinscore.score.PageCrop
import de.citrinscore.score.RecognizedMusic
import de.citrinscore.score.omr.RecognitionPage
import de.citrinscore.score.omr.musicXmlPreview
import de.citrinscore.score.omr.recognizeMusic
import de.citrinscore.score.render.PageSource
import de.citrinscore.score.render.ScoreRenderer
import de.citrinscore.score.render.scanSize
import de.citrinscore.score.scan.EraserStroke
import de.citrinscore.ui.pages.PreviewPage
import de.citrinscore.util.attempt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val OMR_DELAY_MILLIS = 500L

data class OmrPageInput(val key: String, val normalize: Boolean, val crop: PageCrop?, val erased: List<EraserStroke>)

data class OmrInput(val key: String, val pages: List<OmrPageInput>)

sealed interface OmrState {
    val input: Any

    data class Running(override val input: Any, val progress: Float) : OmrState

    data class Done(override val input: Any, val music: RecognizedMusic, val preview: List<PreviewPage>) : OmrState

    data class Failed(override val input: Any) : OmrState
}

sealed interface OmrDisplay {
    data object Idle : OmrDisplay

    data class Recognizing(val progress: Float?) : OmrDisplay

    data object Failed : OmrDisplay

    data class Recognized(val preview: List<PreviewPage>) : OmrDisplay
}

fun omrDisplay(requested: Boolean, ready: Boolean, state: OmrState?, input: Any?): OmrDisplay {
    val current = state?.takeIf { it.input == input }
    return when {
        !requested || !ready -> OmrDisplay.Idle
        current is OmrState.Done -> OmrDisplay.Recognized(current.preview)
        current is OmrState.Failed -> OmrDisplay.Failed
        else -> OmrDisplay.Recognizing((current as? OmrState.Running)?.progress)
    }
}

fun isRecognizing(requested: Boolean, state: OmrState?, input: Any?) =
    requested && state.let { it == null || it is OmrState.Running || it.input != input }

internal fun CoroutineScope.launchMusicRecognition(
    requests: Flow<Any?>,
    current: () -> OmrState?,
    pages: suspend (Any) -> List<RecognitionPage>,
    publish: (Any, OmrState) -> Unit,
) = launch {
    requests.distinctUntilChanged().collectLatest { input ->
        if (input == null) return@collectLatest
        if (current()?.takeIf { it !is OmrState.Running }?.input == input) return@collectLatest
        delay(OMR_DELAY_MILLIS)
        publish(input, OmrState.Running(input, 0f))
        val result = attempt {
            val music = recognizeMusic(pages(input)) { progress -> publish(input, OmrState.Running(input, progress)) }
            OmrState.Done(input, music, musicXmlPreview(music.musicXml))
        }
        publish(input, result ?: OmrState.Failed(input))
    }
}

private const val OMR_RESOLUTION = 2000f

internal suspend fun PageSource.recognitionPage(page: Int, region: Rect): RecognitionPage {
    val scale = OMR_RESOLUTION / region.width
    return RecognitionPage(withContext(Dispatchers.Default) { render(page, region, scanSize(region.size, scale)) }, scale)
}

internal suspend fun ScoreRenderer.recognitionPage(page: Int): RecognitionPage {
    val size = pageSizes[page]
    val scale = OMR_RESOLUTION / size.width
    return RecognitionPage(render(page, Rect(Offset.Zero, size), scanSize(size, scale)), scale)
}
