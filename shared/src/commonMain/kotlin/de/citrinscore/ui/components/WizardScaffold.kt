package de.citrinscore.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.citrinscore.i18n.LocalStrings
import de.citrinscore.resources.Res
import de.citrinscore.resources.ic_arrow_back
import de.citrinscore.resources.ic_arrow_forward
import org.jetbrains.compose.resources.painterResource

val ContentMaxWidth = 880.dp
val WideContentMaxWidth = 1440.dp
private val PagePadding = 20.dp
private val CompactWidth = 480.dp

@Composable
fun <S : Enum<S>> WizardScaffold(
    step: S,
    stepCount: Int,
    backLabel: String?,
    onBack: () -> Unit,
    nextLabel: String,
    nextEnabled: Boolean,
    onNext: () -> Unit,
    contentMaxWidth: (S) -> Dp = { ContentMaxWidth },
    content: @Composable (S) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val navigationMaxWidth by animateDpAsState(contentMaxWidth(step))
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(colors.surface, colors.primaryContainer.copy(alpha = 0.45f))))
            .safeDrawingPadding()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        StepIndicator(step.ordinal, stepCount)
        AnimatedContent(
            targetState = step,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            transitionSpec = {
                val direction = if (targetState > initialState) 1 else -1
                (slideInHorizontally { direction * it / 4 } + fadeIn()) togetherWith
                    (slideOutHorizontally { -direction * it / 4 } + fadeOut())
            },
        ) { current ->
            ScrollableStep(contentMaxWidth(current)) { content(current) }
        }
        NavigationBar(
            backLabel = backLabel,
            onBack = onBack,
            nextLabel = nextLabel,
            nextEnabled = nextEnabled,
            onNext = onNext,
            modifier = Modifier.padding(start = PagePadding, end = PagePadding, top = 16.dp).widthIn(max = navigationMaxWidth),
        )
    }
}

@Composable
private fun ScrollableStep(maxWidth: Dp, content: @Composable () -> Unit) {
    val scrollState = rememberScrollState()
    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(start = PagePadding, end = PagePadding, top = 24.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(Modifier.widthIn(max = maxWidth)) {
                content()
            }
        }
        VerticalScrollIndicator(
            scrollState = scrollState,
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(end = 4.dp),
        )
    }
}

@Composable
private fun StepIndicator(currentIndex: Int, stepCount: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(stepCount) { index ->
                val color by animateColorAsState(
                    if (index <= currentIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                )
                Box(Modifier.size(width = 48.dp, height = 6.dp).background(color, CircleShape))
            }
        }
        Text(
            LocalStrings.current.stepOf(currentIndex + 1, stepCount),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun NavigationBar(
    backLabel: String?,
    onBack: () -> Unit,
    nextLabel: String,
    nextEnabled: Boolean,
    onNext: () -> Unit,
    modifier: Modifier,
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val compact = maxWidth < CompactWidth
        Row(verticalAlignment = Alignment.CenterVertically) {
            BackButton(label = backLabel, compact = compact, onClick = onBack)
            Spacer(Modifier.weight(1f))
            Button(
                onClick = onNext,
                enabled = nextEnabled,
                contentPadding = PaddingValues(horizontal = if (compact) 24.dp else 32.dp, vertical = 18.dp),
            ) {
                Text(nextLabel, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.width(8.dp))
                Icon(painterResource(Res.drawable.ic_arrow_forward), contentDescription = null)
            }
        }
    }
}

@Composable
private fun BackButton(label: String?, compact: Boolean, onClick: () -> Unit) {
    val visible = label != null
    val modifier = Modifier.alpha(if (visible) 1f else 0f)
    val icon = painterResource(Res.drawable.ic_arrow_back)
    if (compact) {
        IconButton(onClick = onClick, enabled = visible, modifier = modifier) {
            Icon(icon, contentDescription = label)
        }
    } else {
        TextButton(
            onClick = onClick,
            enabled = visible,
            modifier = modifier,
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 18.dp),
        ) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(label.orEmpty(), style = MaterialTheme.typography.titleMedium)
        }
    }
}
