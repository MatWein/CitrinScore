package de.citrinscore.ui.viewer.playback

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.citrinscore.i18n.LocalStrings
import de.citrinscore.i18n.Strings
import de.citrinscore.resources.Res
import de.citrinscore.resources.ic_arrow_drop_down
import de.citrinscore.resources.ic_check
import de.citrinscore.resources.ic_close
import de.citrinscore.resources.ic_mic
import de.citrinscore.resources.ic_pause
import de.citrinscore.resources.ic_play
import de.citrinscore.resources.ic_stop
import de.citrinscore.score.playback.Instrument
import de.citrinscore.ui.components.IconAction
import de.citrinscore.ui.theme.Spacing
import de.citrinscore.ui.viewer.audio.rememberMicrophoneAccess
import org.jetbrains.compose.resources.painterResource
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToInt

private val PanelMaxWidth = 420.dp
private val PanelElevation = 6.dp
private val ProgressSize = 24.dp
private val TempoLabelWidth = 48.dp
private const val PERCENT = 100

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackPanel(state: PlaybackState, viewModel: PlaybackViewModel, modifier: Modifier = Modifier) {
    val strings = LocalStrings.current
    val requestMicrophone = rememberMicrophoneAccess(viewModel::start)
    Surface(
        modifier = modifier.widthIn(max = PanelMaxWidth),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = PanelElevation,
    ) {
        Column(Modifier.padding(start = Spacing.Large, end = Spacing.ExtraSmall, top = Spacing.Small, bottom = Spacing.Small)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SingleChoiceSegmentedButtonRow(Modifier.weight(1f)) {
                    PlaybackMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = state.mode == mode,
                            onClick = { viewModel.selectMode(mode) },
                            enabled = mode == PlaybackMode.Listen || state.microphoneAvailable == true,
                            shape = SegmentedButtonDefaults.itemShape(index, PlaybackMode.entries.size),
                        ) { Text(strings.label(mode)) }
                    }
                }
                IconAction(Res.drawable.ic_close, strings.closePlayback, viewModel::close)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                Box(contentAlignment = Alignment.Center) {
                    when {
                        state.isLoading -> CircularProgressIndicator(Modifier.size(ProgressSize))
                        state.mode == PlaybackMode.Listen && state.isRunning -> IconAction(Res.drawable.ic_pause, strings.pause, viewModel::stop)
                        state.mode == PlaybackMode.Listen -> IconAction(Res.drawable.ic_play, strings.play, viewModel::start)
                        state.isRunning -> IconAction(Res.drawable.ic_stop, strings.stopPlayAlong, viewModel::stop)
                        else -> IconAction(Res.drawable.ic_mic, strings.startPlayAlong, requestMicrophone)
                    }
                }
                when (state.mode) {
                    PlaybackMode.Listen -> TempoSlider(state.tempo, viewModel::setTempo, Modifier.weight(1f))
                    PlaybackMode.PlayAlong -> Text(
                        strings.playAlongHint,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f).padding(end = Spacing.Medium),
                    )
                }
            }
            if (state.mode == PlaybackMode.Listen) {
                InstrumentPicker(state.instrument, viewModel::setInstrument, enabled = !state.isLoading)
            }
            state.error?.let { error ->
                Text(
                    strings.message(error),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(end = Spacing.Medium, bottom = Spacing.ExtraSmall),
                )
            }
        }
    }
}

@Composable
private fun TempoSlider(tempo: Float, onTempo: (Float) -> Unit, modifier: Modifier) {
    val strings = LocalStrings.current
    Row(modifier.padding(end = Spacing.Medium), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
        Text(strings.tempo, style = MaterialTheme.typography.labelLarge)
        Slider(
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
private fun InstrumentPicker(selected: Instrument?, onSelect: (Instrument?) -> Unit, enabled: Boolean) {
    val strings = LocalStrings.current
    var expanded by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
        Text(strings.instrument, style = MaterialTheme.typography.labelLarge)
        Box {
            TextButton(onClick = { expanded = true }, enabled = enabled) {
                Text(strings.label(selected))
                Icon(painterResource(Res.drawable.ic_arrow_drop_down), contentDescription = null)
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
