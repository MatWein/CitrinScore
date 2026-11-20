package de.citrinscore.omr

sealed interface OmrProgress {
    val page: Int
    val pages: Int

    data class Segmenting(override val page: Int, override val pages: Int, val patch: Int, val patches: Int) : OmrProgress

    data class ReadingStaff(override val page: Int, override val pages: Int, val staff: Int, val staffs: Int) : OmrProgress
}
