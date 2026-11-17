package de.citrinscore.ui.parts

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import de.citrinscore.i18n.LocalStrings
import de.citrinscore.ui.components.CitrinTextField
import de.citrinscore.ui.theme.ContentWidth

@Composable
fun PartNameField(name: String, taken: Boolean, onChange: (String) -> Unit) {
    val strings = LocalStrings.current
    CitrinTextField(
        value = name,
        onValueChange = onChange,
        label = strings.partNameLabel,
        singleLine = true,
        error = strings.partNameExists.takeIf { taken },
        modifier = Modifier.widthIn(max = ContentWidth.Form).fillMaxWidth(),
    )
}
