package de.citrinscore.ui.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.citrinscore.resources.Res
import de.citrinscore.resources.ic_add
import de.citrinscore.ui.theme.ContentWidth
import de.citrinscore.ui.theme.Spacing
import org.jetbrains.compose.resources.painterResource

private val SideBySideWidth = 720.dp
private val AddButtonPadding = PaddingValues(horizontal = Spacing.ExtraLarge, vertical = 14.dp)
private const val LIST_WEIGHT = 2f
private const val PREVIEW_WEIGHT = 3f

@Composable
fun PagesLayout(list: @Composable (Modifier) -> Unit, preview: @Composable (Modifier) -> Unit) {
    BoxWithConstraints(contentAlignment = Alignment.TopCenter) {
        if (maxWidth >= SideBySideWidth) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Section)) {
                list(Modifier.weight(LIST_WEIGHT))
                preview(Modifier.weight(PREVIEW_WEIGHT))
            }
        } else {
            Column(Modifier.widthIn(max = ContentWidth.Form), verticalArrangement = Arrangement.spacedBy(Spacing.Section)) {
                list(Modifier.fillMaxWidth())
                preview(Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun AddPagesButton(label: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, contentPadding = AddButtonPadding, modifier = Modifier.padding(top = Spacing.Large)) {
        Icon(painterResource(Res.drawable.ic_add), contentDescription = null)
        Spacer(Modifier.width(Spacing.Small))
        Text(label)
    }
}
