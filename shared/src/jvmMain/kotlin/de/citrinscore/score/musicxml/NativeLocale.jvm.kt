package de.citrinscore.score.musicxml

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Platform

private const val C_LOCALE = "C"
private const val WINDOWS_C_RUNTIME = "ucrtbase"
private const val LINUX_LC_NUMERIC = 1
private const val BSD_LC_NUMERIC = 4

private interface CLibrary : Library {
    fun setlocale(category: Int, locale: String): String?
}

internal actual fun useLocaleIndependentNumbers() {
    val library = Native.load(if (Platform.isWindows()) WINDOWS_C_RUNTIME else Platform.C_LIBRARY_NAME, CLibrary::class.java)
    checkNotNull(library.setlocale(if (Platform.isLinux()) LINUX_LC_NUMERIC else BSD_LC_NUMERIC, C_LOCALE)) { "The numeric locale could not be set" }
}
