package de.citrinscore.ui.importer

import de.citrinscore.score.scan.DEFAULT_SCAN_THRESHOLD
import de.citrinscore.ui.pages.PreviewPage
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.path
import kotlin.uuid.Uuid

data class PartDraft(
    val key: String = Uuid.random().toString(),
    val name: String = "",
    val type: ImportType? = null,
    val files: List<PlatformFile> = emptyList(),
    val previews: Map<String, List<PreviewPage>> = emptyMap(),
    val failedPreviews: Set<String> = emptySet(),
    val scanThreshold: Float = DEFAULT_SCAN_THRESHOLD,
) {
    val pages get() = files.flatMap { previews[it.path].orEmpty() }

    val isLoadingPreview get() = files.any { it.path !in previews && it.path !in failedPreviews }

    val previewFailed get() = files.any { it.path in failedPreviews }

    val isComplete get() = files.isNotEmpty() && !isLoadingPreview && !previewFailed

    val pageCount get() = pages.size.takeUnless { type == ImportType.MusicXml }
}

fun partNameTaken(name: String, otherNames: List<String>): Boolean {
    val trimmed = name.trim()
    return trimmed.isNotEmpty() && otherNames.any { it.trim().equals(trimmed, ignoreCase = true) }
}
