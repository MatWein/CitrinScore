package de.citrinscore.ui.importer

actual val ImportType.pickerExtensions: List<String>?
    get() = extensions.takeUnless { this == ImportType.MusicXml }
