package de.citrinscore.library

import io.github.vinceglb.filekit.PlatformFile

/** Whether the system file manager can show this folder; folders inside the private app storage are hidden from it. */
expect val PlatformFile.canShowInFileManager: Boolean

/** Opens this folder in the system file manager. */
expect suspend fun PlatformFile.showInFileManager()
