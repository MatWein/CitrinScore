package de.citrinscore.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.produceLibraries
import com.mikepenz.aboutlibraries.ui.compose.style.LibraryActionBadges
import de.citrinscore.i18n.LocalStrings
import de.citrinscore.resources.Res
import de.citrinscore.resources.ic_arrow_back
import de.citrinscore.ui.components.IconAction
import de.citrinscore.ui.components.VerticalScrollIndicator
import de.citrinscore.ui.theme.ContentWidth
import de.citrinscore.ui.theme.Spacing

private const val LIBRARIES_RESOURCE = "files/aboutlibraries.json"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesScreen(onClose: () -> Unit) {
    val strings = LocalStrings.current
    val libraries by produceLibraries { Res.readBytes(LIBRARIES_RESOURCE).decodeToString() }
    val listState = rememberLazyListState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.openSourceLibraries) },
                navigationIcon = { IconAction(Res.drawable.ic_arrow_back, strings.back, onClick = onClose) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            LibrariesContainer(
                libraries = libraries,
                modifier = Modifier.widthIn(max = ContentWidth.Form).fillMaxWidth(),
                lazyListState = listState,
                actionLabels = LibraryActionBadges(
                    sponsorEnabled = false,
                    source = strings.sourceCode,
                    website = strings.website,
                    viewLicense = strings.viewLicense,
                ),
                licenseDialogConfirmText = strings.ok,
            )
            VerticalScrollIndicator(listState, Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(end = Spacing.ExtraSmall))
        }
    }
}
