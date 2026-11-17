package de.citrinscore.ui.viewer.audio

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import de.citrinscore.i18n.LocalStrings
import de.citrinscore.resources.Res
import de.citrinscore.resources.ic_mic
import de.citrinscore.resources.ic_stop
import de.citrinscore.ui.theme.Spacing
import de.citrinscore.ui.viewer.ViewerBar
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.jetbrains.compose.resources.painterResource
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

private val ClockInterval = 200.milliseconds

@Composable
fun RecordingBar(start: TimeSource.Monotonic.ValueTimeMark, onStop: () -> Unit, modifier: Modifier = Modifier) {
    val strings = LocalStrings.current
    var elapsed by remember(start) { mutableStateOf(Duration.ZERO) }
    LaunchedEffect(start) {
        while (isActive) {
            elapsed = start.elapsedNow()
            delay(ClockInterval)
        }
    }
    ViewerBar(strings.stopRecording, onStop, modifier, closeIcon = Res.drawable.ic_stop) {
        Icon(
            painterResource(Res.drawable.ic_mic),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(start = Spacing.Medium),
        )
        Text(strings.recordingActive, style = MaterialTheme.typography.labelLarge)
        Text(elapsed.clock(), style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.weight(1f))
    }
}
