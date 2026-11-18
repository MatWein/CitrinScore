package de.citrinscore.library

import de.citrinscore.util.attempt
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.copyRecursively
import io.github.vinceglb.filekit.delete
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.list
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

enum class LibraryMoveResult { Moved, SourceKept, TargetOccupied, Failed }

suspend fun moveLibrary(
    source: PlatformFile,
    target: PlatformFile,
    discarded: Set<String>,
    commit: suspend () -> Unit,
): LibraryMoveResult = withContext(Dispatchers.IO) {
    if (source.path == target.path) {
        return@withContext if (attempt { commit() } != null) LibraryMoveResult.Moved else LibraryMoveResult.Failed
    }
    val (existing, occupied) = attempt { source.list() to target.list().mapTo(HashSet()) { it.name } } ?: return@withContext LibraryMoveResult.Failed
    val entries = existing.filter { it.name !in discarded }
    if (entries.any { it.name in occupied }) return@withContext LibraryMoveResult.TargetOccupied

    val copies = entries.map { target / it.name }
    val committed = attempt {
        entries.zip(copies).forEach { (entry, copy) -> entry.copyRecursively(copy) }
        commit()
    }
    if (committed == null) {
        copies.forEach { attempt { it.delete(mustExist = false, recursively = true) } }
        return@withContext LibraryMoveResult.Failed
    }

    val removed = existing.map { attempt { it.delete(mustExist = false, recursively = true) } != null }
    if (removed.all { it }) LibraryMoveResult.Moved else LibraryMoveResult.SourceKept
}
