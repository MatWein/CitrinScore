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
    val scans: Map<String, List<ScanPreview>> = emptyMap(),
    val erasedPages: List<ScanPageRef> = emptyList(),
) {
    val pages get() = files.flatMap { previews[it.path].orEmpty() }

    val scanPages get() = files.flatMap { file -> scans[file.path].orEmpty().mapIndexed { index, scan -> ScanPageRef(file.path, index) to scan } }

    val isLoadingPreview get() = files.any { it.path !in previews && it.path !in scans && it.path !in failedPreviews }

    val previewFailed get() = files.any { it.path in failedPreviews }

    val isComplete get() = files.isNotEmpty() && !isLoadingPreview && !previewFailed

    val pageCount get() = when (type) {
        ImportType.MusicXml -> null
        ImportType.Scan -> scanPages.size
        else -> pages.size
    }

    fun withScan(page: ScanPageRef, transform: (ScanPreview) -> ScanPreview) = copy(
        scans = scans + (page.path to scans.getValue(page.path).mapIndexed { index, scan -> if (index == page.index) transform(scan) else scan }),
    )
}

fun partNameTaken(name: String, otherNames: List<String>): Boolean {
    val trimmed = name.trim()
    return trimmed.isNotEmpty() && otherNames.any { it.trim().equals(trimmed, ignoreCase = true) }
}
