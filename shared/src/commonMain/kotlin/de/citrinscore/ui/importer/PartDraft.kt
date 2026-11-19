package de.citrinscore.ui.importer

import de.citrinscore.i18n.Strings
import de.citrinscore.score.scan.DEFAULT_SCAN_THRESHOLD
import de.citrinscore.ui.parts.partSummary
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
    val omrEnabled: Boolean = false,
    val omr: OmrState? = null,
) {
    val pages get() = files.flatMap { previews[it.path].orEmpty() }

    val scanPages get() = files.flatMap { file -> scans[file.path].orEmpty().mapIndexed { index, scan -> ScanPageRef(file.path, index) to scan } }

    val isLoadingPreview get() = files.any { it.path !in previews && it.path !in scans && it.path !in failedPreviews }

    val previewFailed get() = files.any { it.path in failedPreviews }

    val isPreviewReady get() = files.isNotEmpty() && !isLoadingPreview && !previewFailed

    val supportsOmr get() = type?.supportsOmr == true

    val isOmrRequested get() = omrEnabled && supportsOmr

    val omrInput get() = OmrInput(
        key = key,
        files = files.map { it.path },
        crops = pages.map { it.crop },
        scans = scanPages.map { (page, scan) -> OmrScan(page, scan.crop, scan.erased) },
        scanThreshold = scanThreshold,
    )

    val recognized get() = (omr as? OmrState.Done)?.takeIf { isOmrRequested && it.input == omrInput }

    val isRecognizing get() = isRecognizing(isOmrRequested, omr, omrInput)

    val omrDisplay get() = omrDisplay(isOmrRequested, isPreviewReady, omr, omrInput)

    val isComplete get() = isPreviewReady && !isRecognizing

    val pageCount get() = when (type) {
        ImportType.MusicXml -> null
        ImportType.Scan -> scanPages.size
        else -> pages.size
    }

    fun withScan(page: ScanPageRef, transform: (ScanPreview) -> ScanPreview) = copy(
        scans = scans + (page.path to scans.getValue(page.path).mapIndexed { index, scan -> if (index == page.index) transform(scan) else scan }),
    )
}

fun PartDraft.summary(strings: Strings): String {
    val summary = strings.partSummary(type?.title(strings).orEmpty(), pageCount)
    return if (recognized == null) summary else "$summary · ${strings.omrPartSummary}"
}

fun partNameTaken(name: String, otherNames: List<String>): Boolean {
    val trimmed = name.trim()
    return trimmed.isNotEmpty() && otherNames.any { it.trim().equals(trimmed, ignoreCase = true) }
}
