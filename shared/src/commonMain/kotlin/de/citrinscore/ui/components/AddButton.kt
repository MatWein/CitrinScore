package de.citrinscore.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.citrinscore.resources.Res
import de.citrinscore.resources.ic_add
import de.citrinscore.ui.theme.Spacing
import org.jetbrains.compose.resources.painterResource

private val AddButtonPadding = PaddingValues(horizontal = Spacing.ExtraLarge, vertical = 14.dp)

@Composable
fun AddButton(label: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, contentPadding = AddButtonPadding, modifier = Modifier.padding(top = Spacing.Large)) {
        Icon(painterResource(Res.drawable.ic_add), contentDescription = null)
        Spacer(Modifier.width(Spacing.Small))
        Text(label)
    }
}
