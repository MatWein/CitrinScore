package de.citrinscore.ui.viewer.audio

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.citrinscore.i18n.LocalStrings
import de.citrinscore.resources.Res
import de.citrinscore.resources.ic_close
import de.citrinscore.resources.ic_pause
import de.citrinscore.resources.ic_play
import de.citrinscore.resources.ic_stop
import de.citrinscore.ui.components.IconAction
import de.citrinscore.ui.theme.Spacing
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

private val PlayerWidth = 320.dp
private val PlayerElevation = 6.dp
private const val MIN_SLIDER_SECONDS = 0.001f

@Composable
fun AudioPlayerCard(player: PlayerState, viewModel: ScoreAudioViewModel, modifier: Modifier = Modifier) {
    val strings = LocalStrings.current
    var dragged by remember(player.attachment) { mutableStateOf<Float?>(null) }
    Surface(
        modifier = modifier.width(PlayerWidth),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = PlayerElevation,
    ) {
        Column(Modifier.padding(start = Spacing.Large, end = Spacing.ExtraSmall, top = Spacing.ExtraSmall, bottom = Spacing.ExtraSmall)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    player.attachment.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                IconAction(Res.drawable.ic_close, strings.closePlayer, viewModel::closePlayer)
            }
            Slider(
                value = dragged ?: player.position.toDouble(DurationUnit.SECONDS).toFloat(),
                onValueChange = { dragged = it },
                onValueChangeFinished = {
                    dragged?.let { viewModel.seekTo(it.toDouble().seconds) }
                    dragged = null
                },
                valueRange = 0f..player.duration.toDouble(DurationUnit.SECONDS).toFloat().coerceAtLeast(MIN_SLIDER_SECONDS),
                modifier = Modifier.padding(end = Spacing.Medium),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (player.isPlaying) {
                    IconAction(Res.drawable.ic_pause, strings.pause, viewModel::pause)
                } else {
                    IconAction(Res.drawable.ic_play, strings.play, viewModel::play)
                }
                IconAction(Res.drawable.ic_stop, strings.stop, viewModel::stop)
                Spacer(Modifier.weight(1f))
                Text(
                    "${(dragged?.toDouble()?.seconds ?: player.position).clock()} / ${player.duration.clock()}",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(end = Spacing.Medium),
                )
            }
        }
    }
}
