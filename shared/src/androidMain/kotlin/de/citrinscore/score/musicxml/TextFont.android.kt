package de.citrinscore.score.musicxml

import android.graphics.Typeface
import com.caverock.androidsvg.SVG
import com.caverock.androidsvg.SVGExternalFileResolver
import java.io.File

internal actual fun registerTextFont(family: String, file: File) {
    val typeface = Typeface.createFromFile(file)
    SVG.registerExternalFileResolver(
        object : SVGExternalFileResolver() {
            override fun resolveFont(fontFamily: String?, fontWeight: Int, fontStyle: String?): Typeface? =
                typeface.takeIf { fontFamily == family }
        },
    )
}
