package de.citrinscore.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import de.citrinscore.AppInfo
import de.citrinscore.i18n.LocalStrings
import de.citrinscore.resources.Res
import de.citrinscore.resources.app_icon
import de.citrinscore.resources.ic_arrow_back
import de.citrinscore.ui.components.IconAction
import de.citrinscore.ui.components.VerticalScrollIndicator
import de.citrinscore.ui.theme.ContentWidth
import de.citrinscore.ui.theme.Spacing
import org.jetbrains.compose.resources.painterResource

private val AppIconSize = 112.dp
private const val LICENSE_NAME = "GNU General Public License v3.0"
private const val LICENSE_URL = "https://www.gnu.org/licenses/gpl-3.0.html"
private const val BULLET = "•"

private val OpenSourceLibraries = listOf(
    "Compose Multiplatform",
    "FileKit",
    "kotlinx.serialization",
    "Verovio",
    "FluidSynth",
    "GeneralUser GS – S. Christian Collins",
    "JNA",
    "ktmidi",
    "JGit",
    "Apache PDFBox",
    "Apache Batik",
    "AndroidSVG",
    "JLayer / MP3SPI",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onClose: () -> Unit) {
    val strings = LocalStrings.current
    val scrollState = rememberScrollState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.aboutTitle) },
                navigationIcon = { IconAction(Res.drawable.ic_arrow_back, strings.back, onClick = onClose) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(Spacing.Page),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(Modifier.widthIn(max = ContentWidth.Form).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.Section)) {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                        Image(painterResource(Res.drawable.app_icon), contentDescription = null, modifier = Modifier.size(AppIconSize))
                        Text(AppInfo.NAME, style = MaterialTheme.typography.headlineMedium)
                        Text(strings.appVersion(AppInfo.VERSION), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(strings.buildDate(AppInfo.BUILD_DATE), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(strings.createdBy(AppInfo.AUTHOR), style = MaterialTheme.typography.bodyLarge)
                    }
                    SettingsSection(strings.sourceCode) {
                        LinkText(AppInfo.SOURCE_URL, AppInfo.SOURCE_URL)
                    }
                    SettingsSection(strings.license) {
                        Text(strings.licenseText, style = MaterialTheme.typography.bodyLarge)
                        LinkText(LICENSE_NAME, LICENSE_URL)
                    }
                    SettingsSection(strings.openSourceLibraries) {
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall)) {
                            OpenSourceLibraries.forEach { Text("$BULLET $it", style = MaterialTheme.typography.bodyLarge) }
                        }
                    }
                }
            }
            VerticalScrollIndicator(scrollState, Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(end = Spacing.ExtraSmall))
        }
    }
}

@Composable
private fun LinkText(text: String, url: String) {
    val style = TextLinkStyles(SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline))
    Text(
        buildAnnotatedString { withLink(LinkAnnotation.Url(url, style)) { append(text) } },
        style = MaterialTheme.typography.bodyLarge,
    )
}
