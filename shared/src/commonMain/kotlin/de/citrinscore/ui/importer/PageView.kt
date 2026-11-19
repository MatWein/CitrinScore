package de.citrinscore.ui.importer

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import de.citrinscore.score.PageCrop
import de.citrinscore.score.scan.EraserStroke

data class PageView(val image: ImageBitmap, val size: Size, val crop: PageCrop, val erased: List<EraserStroke>)

enum class PageTool { Crop, Eraser }
