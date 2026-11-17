package de.citrinscore.score.musicxml

import java.awt.Font
import java.awt.GraphicsEnvironment
import java.io.File

internal actual fun registerTextFont(family: String, file: File) {
    val font = Font.createFont(Font.TRUETYPE_FONT, file)
    check(font.family == family) { "${file.name} contains ${font.family} instead of $family" }
    GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font)
}
