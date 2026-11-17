package de.citrinscore.ui.viewer.playback

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.citrinscore.i18n.LocalStrings
import de.citrinscore.i18n.Strings
import de.citrinscore.resources.Res
import de.citrinscore.resources.ic_arrow_drop_down
import de.citrinscore.resources.ic_check
import de.citrinscore.resources.ic_library_music
import de.citrinscore.resources.ic_mic
import de.citrinscore.resources.ic_music_note
import de.citrinscore.resources.ic_pause
import de.citrinscore.resources.ic_play
import de.citrinscore.resources.ic_stop
import de.citrinscore.score.playback.Instrument
import de.citrinscore.ui.components.IconAction
import de.citrinscore.ui.components.LabelTooltip
import de.citrinscore.ui.theme.Spacing
import de.citrinscore.ui.viewer.BarSlider
import de.citrinscore.ui.viewer.ViewerBar
import de.citrinscore.ui.viewer.audio.rememberMicrophoneAccess
import org.jetbrains.compose.resources.painterResource
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToInt

private val ProgressSize = 20.dp
private val InstrumentButtonPadding = PaddingValues(horizontal = Spacing.Small)
private val TempoLabelWidth = 48.dp
private const val PERCENT = 100

@Composable
fun PlaybackBar(state: PlaybackState, viewModel: PlaybackViewModel, modifier: Modifier = Modifier) {
    val strings = LocalStrings.current
    val requestMicrophone = rememberMicrophoneAccess(viewModel::start)
    ViewerBar(strings.closePlayback, viewModel::close, modifier) { compact ->
        ModeSelector(state, viewModel::selectMode)
        Box(contentAlignment = Alignment.Center) {
            when {
                state.isLoading -> CircularProgressIndicator(Modifier.size(ProgressSize))
                state.mode == PlaybackMode.Listen && state.isRunning -> IconAction(Res.drawable.ic_pause, strings.pause, viewModel::stop)
                state.mode == PlaybackMode.Listen -> IconAction(Res.drawable.ic_play, strings.play, viewModel::start)
                state.isRunning -> IconAction(Res.drawable.ic_stop, strings.stopPlayAlong, viewModel::stop)
                else -> IconAction(Res.drawable.ic_play, strings.startPlayAlong, requestMicrophone)
            }
        }
        val error = state.error
        when {
            error != null -> BarText(strings.message(error), MaterialTheme.colorScheme.error, Modifier.weight(1f))
            state.mode == PlaybackMode.Listen -> {
                TempoSlider(state.tempo, viewModel::setTempo, showLabel = !compact, modifier = Modifier.weight(1f))
                InstrumentPicker(state.instrument, viewModel::setInstrument, enabled = !state.isLoading, compact = compact)
            }
            else -> BarText(strings.playAlongHint, MaterialTheme.colorScheme.onSurfaceVariant, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ModeSelector(state: PlaybackState, onSelect: (PlaybackMode) -> Unit) {
    val strings = LocalStrings.current
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconAction(state.mode.icon, strings.label(state.mode), onClick = { expanded = true })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            PlaybackMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(strings.label(mode)) },
                    onClick = {
                        expanded = false
                        onSelect(mode)
                    },
                    leadingIcon = { Icon(painterResource(mode.icon), contentDescription = null) },
                    trailingIcon = { if (mode == state.mode) Icon(painterResource(Res.drawable.ic_check), contentDescription = null) },
                    enabled = state.isAvailable(mode),
                )
            }
        }
    }
}

private fun PlaybackState.isAvailable(mode: PlaybackMode) = mode == PlaybackMode.Listen || microphoneAvailable == true

@Composable
private fun BarText(text: String, color: Color, modifier: Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

@Composable
private fun TempoSlider(tempo: Float, onTempo: (Float) -> Unit, showLabel: Boolean, modifier: Modifier) {
    val strings = LocalStrings.current
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
        if (showLabel) Text(strings.tempo, style = MaterialTheme.typography.labelLarge)
        BarSlider(
            value = ln(tempo),
            onValueChange = { onTempo(exp(it)) },
            valueRange = ln(MIN_TEMPO)..ln(MAX_TEMPO),
            modifier = Modifier.weight(1f),
        )
        Text(
            strings.tempoPercent((tempo * PERCENT).roundToInt()),
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.End,
            modifier = Modifier.widthIn(min = TempoLabelWidth),
        )
    }
}

@Composable
private fun InstrumentPicker(selected: Instrument?, onSelect: (Instrument?) -> Unit, enabled: Boolean, compact: Boolean) {
    val strings = LocalStrings.current
    var expanded by remember { mutableStateOf(false) }
    Box {
        if (compact) {
            IconAction(Res.drawable.ic_library_music, strings.instrument, onClick = { expanded = true }, enabled = enabled)
        } else {
            LabelTooltip(strings.instrument) {
                TextButton(onClick = { expanded = true }, enabled = enabled, contentPadding = InstrumentButtonPadding) {
                    Text(strings.label(selected), maxLines = 1)
                    Icon(painterResource(Res.drawable.ic_arrow_drop_down), contentDescription = null)
                }
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            (listOf(null) + Instrument.entries).forEach { instrument ->
                DropdownMenuItem(
                    text = { Text(strings.label(instrument)) },
                    onClick = {
                        expanded = false
                        onSelect(instrument)
                    },
                    trailingIcon = {
                        if (instrument == selected) Icon(painterResource(Res.drawable.ic_check), contentDescription = null)
                    },
                )
            }
        }
    }
}

private val PlaybackMode.icon
    get() = when (this) {
        PlaybackMode.Listen -> Res.drawable.ic_music_note
        PlaybackMode.PlayAlong -> Res.drawable.ic_mic
    }

private fun Strings.label(instrument: Instrument?) = instrument?.let(::instrumentName) ?: notatedInstrument

private fun Strings.label(mode: PlaybackMode) = when (mode) {
    PlaybackMode.Listen -> listenMode
    PlaybackMode.PlayAlong -> playAlongMode
}

private fun Strings.message(error: PlaybackError) = when (error) {
    PlaybackError.Score -> playbackScoreFailed
    PlaybackError.Synthesizer -> playbackSynthesizerFailed
    PlaybackError.Microphone -> playbackMicrophoneFailed
}
