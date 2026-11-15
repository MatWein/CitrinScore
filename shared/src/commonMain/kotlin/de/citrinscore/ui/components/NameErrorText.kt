package de.citrinscore.ui.components

import androidx.compose.runtime.Composable
import de.citrinscore.i18n.LocalStrings
import de.citrinscore.library.NameError

val NameError.text: String
    @Composable get() {
        val strings = LocalStrings.current
        return when (this) {
            NameError.Empty -> strings.nameEmpty
            NameError.InvalidCharacters -> strings.nameInvalid
            NameError.ReservedExtension -> strings.nameReservedExtension
            NameError.AlreadyExists -> strings.nameExists
        }
    }
