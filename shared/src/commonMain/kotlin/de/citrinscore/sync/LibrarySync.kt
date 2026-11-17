package de.citrinscore.sync

import de.citrinscore.settings.GitRemote
import io.github.vinceglb.filekit.PlatformFile

interface LibrarySync {
    suspend fun connect(library: PlatformFile, remote: GitRemote)

    suspend fun synchronize(
        library: PlatformFile,
        remote: GitRemote,
        progress: (SyncProgress) -> Unit,
        resolve: suspend (List<SyncConflict>) -> Map<String, ConflictChoice>?,
    ): SyncOutcome
}

expect fun librarySync(): LibrarySync?

enum class SyncStep { Committing, Downloading, Merging, Uploading }

data class SyncProgress(val step: SyncStep, val fraction: Float? = null)

data class SyncConflict(val path: String, val folder: String, val title: String)

enum class ConflictChoice { Local, Remote }

sealed interface SyncOutcome {
    data class Completed(val uploaded: Int, val downloaded: Int) : SyncOutcome

    data object Cancelled : SyncOutcome
}

enum class SyncFailure { StorageAccess, LibraryLocation, Connection, Rejected, Failed }

class SyncException(val failure: SyncFailure, message: String?, cause: Throwable? = null) : Exception(message, cause)
