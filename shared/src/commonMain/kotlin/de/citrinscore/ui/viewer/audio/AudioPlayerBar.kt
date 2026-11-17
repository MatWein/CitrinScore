package de.citrinscore.ui.viewer.audio

import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.citrinscore.i18n.LocalStrings
import de.citrinscore.resources.Res
import de.citrinscore.resources.ic_pause
import de.citrinscore.resources.ic_play
import de.citrinscore.resources.ic_stop
import de.citrinscore.ui.components.IconAction
import de.citrinscore.ui.viewer.BarSlider
import de.citrinscore.ui.viewer.ViewerBar
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

private val NameMaxWidth = 200.dp
private const val MIN_SLIDER_SECONDS = 0.001f

@Composable
fun AudioPlayerBar(player: PlayerState, viewModel: ScoreAudioViewModel, modifier: Modifier = Modifier) {
    val strings = LocalStrings.current
    var dragged by remember(player.attachment) { mutableStateOf<Float?>(null) }
    ViewerBar(strings.closePlayer, viewModel::closePlayer, modifier) { compact ->
        if (player.isPlaying) {
            IconAction(Res.drawable.ic_pause, strings.pause, viewModel::pause)
        } else {
            IconAction(Res.drawable.ic_play, strings.play, viewModel::play)
        }
        IconAction(Res.drawable.ic_stop, strings.stop, viewModel::stop)
        if (!compact) {
            Text(
                player.attachment.name,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = NameMaxWidth),
            )
        }
        BarSlider(
            value = dragged ?: player.position.toDouble(DurationUnit.SECONDS).toFloat(),
            onValueChange = { dragged = it },
            onValueChangeFinished = {
                dragged?.let { viewModel.seekTo(it.toDouble().seconds) }
                dragged = null
            },
            valueRange = 0f..player.duration.toDouble(DurationUnit.SECONDS).toFloat().coerceAtLeast(MIN_SLIDER_SECONDS),
            modifier = Modifier.weight(1f),
        )
        Text(
            "${(dragged?.toDouble()?.seconds ?: player.position).clock()} / ${player.duration.clock()}",
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
