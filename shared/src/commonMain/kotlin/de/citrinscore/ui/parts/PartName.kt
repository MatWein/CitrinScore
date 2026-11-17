package de.citrinscore.ui.parts

import de.citrinscore.i18n.Strings

fun Strings.partName(name: String, index: Int) = name.ifEmpty { partNumber(index + 1) }

fun Strings.partSummary(type: String, pages: Int) = "$type · ${pageCount(pages)}"
