package de.citrinscore.settings

import io.github.vinceglb.filekit.PlatformFile

sealed interface LibraryTarget {
    data object App : LibraryTarget

    data class Folder(val directory: PlatformFile) : LibraryTarget

    suspend fun location(): LibraryLocation = when (this) {
        App -> LibraryLocation.App
        is Folder -> LibraryLocation.Folder.of(directory)
    }

    suspend fun directory(): PlatformFile = when (this) {
        App -> LibraryLocation.App.resolve()
        is Folder -> directory
    }
}
