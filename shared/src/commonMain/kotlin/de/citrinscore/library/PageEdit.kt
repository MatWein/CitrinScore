package de.citrinscore.library

import de.citrinscore.score.PageCrop
import io.github.vinceglb.filekit.PlatformFile

sealed interface PageEdit {
    data class Keep(val index: Int) : PageEdit

    data class Add(val image: PlatformFile, val crop: PageCrop) : PageEdit
}
