package de.citrinscore.ui.importer

import de.citrinscore.score.PageCrop
import de.citrinscore.score.scan.EraserStroke
import de.citrinscore.ui.pages.PreviewPage

data class OmrScan(val page: ScanPageRef, val crop: PageCrop, val erased: List<EraserStroke>)

data class OmrInput(val files: List<String>, val crops: List<PageCrop>, val scans: List<OmrScan>)

sealed interface OmrState {
    val input: OmrInput

    data class Running(override val input: OmrInput, val progress: Float) : OmrState

    data class Done(override val input: OmrInput, val musicXml: String, val preview: List<PreviewPage>) : OmrState

    data class Failed(override val input: OmrInput) : OmrState
}
