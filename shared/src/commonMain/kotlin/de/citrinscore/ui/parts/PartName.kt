package de.citrinscore.ui.parts

import de.citrinscore.i18n.Strings

fun Strings.partName(name: String, index: Int) = name.ifEmpty { partNumber(index + 1) }
