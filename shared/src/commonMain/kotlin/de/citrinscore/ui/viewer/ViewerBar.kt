package de.citrinscore.ui.viewer

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import de.citrinscore.resources.Res
import de.citrinscore.resources.ic_close
import de.citrinscore.ui.components.IconAction
import de.citrinscore.ui.theme.Spacing
import org.jetbrains.compose.resources.DrawableResource

val ViewerBarHeight = TopAppBarDefaults.TopAppBarExpandedHeight / 2
private val CompactBarWidth = 600.dp
private val BarThumbSize = DpSize(4.dp, 20.dp)
private val BarTrackHeight = 6.dp

@Composable
fun ViewerBar(
    closeLabel: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    closeIcon: DrawableResource = Res.drawable.ic_close,
    content: @Composable RowScope.(compact: Boolean) -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = modifier.fillMaxWidth()) {
        BoxWithConstraints {
            val compact = maxWidth < CompactBarWidth
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ViewerBarHeight)
                    .padding(horizontal = Spacing.ExtraSmall),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
            ) {
                content(compact)
                IconAction(closeIcon, closeLabel, onClose)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        onValueChangeFinished = onValueChangeFinished,
        interactionSource = interactionSource,
        thumb = { SliderDefaults.Thumb(interactionSource = interactionSource, thumbSize = BarThumbSize) },
        track = { state -> SliderDefaults.Track(sliderState = state, modifier = Modifier.height(BarTrackHeight)) },
        valueRange = valueRange,
    )
}
