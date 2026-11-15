package de.citrinscore.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.citrinscore.i18n.LocalStrings
import de.citrinscore.resources.Res
import de.citrinscore.resources.ic_arrow_back
import de.citrinscore.resources.ic_arrow_forward
import de.citrinscore.settings.AppSettings
import de.citrinscore.ui.components.VerticalScrollIndicator
import org.jetbrains.compose.resources.painterResource

private val ContentMaxWidth = 880.dp
private val PagePadding = 20.dp
private val CompactWidth = 480.dp

@Composable
fun OnboardingScreen(
    onFinished: (AppSettings) -> Unit,
    viewModel: OnboardingViewModel = viewModel { OnboardingViewModel() },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme

    CompositionLocalProvider(LocalStrings provides state.language.strings) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(colors.surface, colors.primaryContainer.copy(alpha = 0.45f))))
                .safeDrawingPadding()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            StepIndicator(state.step)
            AnimatedContent(
                targetState = state.step,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                transitionSpec = {
                    val direction = if (targetState > initialState) 1 else -1
                    (slideInHorizontally { direction * it / 4 } + fadeIn()) togetherWith
                        (slideOutHorizontally { -direction * it / 4 } + fadeOut())
                },
            ) { step ->
                StepContent(step, state, viewModel)
            }
            NavigationBar(
                state = state,
                onBack = viewModel::back,
                onNext = { viewModel.next(onFinished) },
                modifier = Modifier.padding(start = PagePadding, end = PagePadding, top = 16.dp),
            )
        }
    }
}

@Composable
private fun StepContent(step: OnboardingStep, state: OnboardingState, viewModel: OnboardingViewModel) {
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
            Box(Modifier.widthIn(max = ContentMaxWidth)) {
                when (step) {
                    OnboardingStep.Welcome -> WelcomeStep()
                    OnboardingStep.Language -> LanguageStep(state.language, viewModel::selectLanguage)
                    OnboardingStep.Library -> LibraryStep(
                        library = state.library,
                        showError = state.libraryError,
                        onSelect = viewModel::selectLibrary,
                        onError = viewModel::reportLibraryError,
                    )
                }
            }
        }
        VerticalScrollIndicator(
            scrollState = scrollState,
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(end = 4.dp),
        )
    }
}

@Composable
private fun StepIndicator(step: OnboardingStep) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OnboardingStep.entries.forEach { entry ->
                val color by animateColorAsState(
                    if (entry <= step) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                )
                Box(Modifier.size(width = 48.dp, height = 6.dp).background(color, CircleShape))
            }
        }
        Text(
            LocalStrings.current.stepOf(step.number, OnboardingStep.entries.size),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun NavigationBar(state: OnboardingState, onBack: () -> Unit, onNext: () -> Unit, modifier: Modifier) {
    val strings = LocalStrings.current
    BoxWithConstraints(modifier.widthIn(max = ContentMaxWidth).fillMaxWidth()) {
        val compact = maxWidth < CompactWidth
        Row(verticalAlignment = Alignment.CenterVertically) {
            BackButton(
                label = strings.back,
                compact = compact,
                visible = !state.step.isFirst,
                onClick = onBack,
            )
            Spacer(Modifier.weight(1f))
            Button(
                onClick = onNext,
                enabled = state.canAdvance,
                contentPadding = PaddingValues(horizontal = if (compact) 24.dp else 32.dp, vertical = 18.dp),
            ) {
                Text(if (state.step.isLast) strings.getStarted else strings.next, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.width(8.dp))
                Icon(painterResource(Res.drawable.ic_arrow_forward), contentDescription = null)
            }
        }
    }
}

@Composable
private fun BackButton(label: String, compact: Boolean, visible: Boolean, onClick: () -> Unit) {
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
            Text(label, style = MaterialTheme.typography.titleMedium)
        }
    }
}
