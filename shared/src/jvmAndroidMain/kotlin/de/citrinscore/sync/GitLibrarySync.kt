package de.citrinscore.sync

import de.citrinscore.score.CitScore
import de.citrinscore.settings.GitRemote
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.MergeCommand.FastForwardMode
import org.eclipse.jgit.api.MergeResult.MergeStatus
import org.eclipse.jgit.api.ResetCommand.ResetType
import org.eclipse.jgit.api.errors.GitAPIException
import org.eclipse.jgit.api.errors.InvalidRemoteException
import org.eclipse.jgit.api.errors.TransportException
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.lib.ConfigConstants
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.lib.ProgressMonitor
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.merge.MergeStrategy
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.transport.RefSpec
import org.eclipse.jgit.transport.RemoteRefUpdate
import org.eclipse.jgit.transport.URIish
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.eclipse.jgit.treewalk.TreeWalk
import org.eclipse.jgit.util.io.DisabledOutputStream
import java.io.File
import java.io.IOException
import java.net.URISyntaxException

private const val REMOTE = "origin"
private const val DEFAULT_BRANCH = "main"
private const val TOKEN_USERNAME = "oauth2"
private const val IGNORE_FILE = ".gitignore"
private const val KEEP_FILE = ".gitkeep"
private const val AUTHOR_EMAIL = "sync@citrinscore.invalid"
private const val PATH_SEPARATOR = "/"
private const val TIMEOUT_SECONDS = 30
private val IgnoredPatterns = listOf("**/*.${CitScore.EXTENSION}/${CitScore.DEVICE_STATE_FILE}")
private val AcceptedPushes = setOf(RemoteRefUpdate.Status.OK, RemoteRefUpdate.Status.UP_TO_DATE)

actual fun librarySync(): LibrarySync? = GitLibrarySync

internal object GitLibrarySync : LibrarySync {

    override val syncsFolders = foldersHaveWorkingDirectory

    override val metadata = setOf(Constants.DOT_GIT)

    override suspend fun connect(library: PlatformFile, remote: GitRemote) = withContext(Dispatchers.IO) {
        failingAs {
            val directory = library.workingDirectory()
            Git.lsRemoteRepository().setRemote(remote.url).setCredentialsProvider(remote.credentials).setTimeout(TIMEOUT_SECONDS).call()
            open(directory).close()
        }
    }

    override suspend fun synchronize(
        library: PlatformFile,
        remote: GitRemote,
        progress: (SyncProgress) -> Unit,
        resolve: suspend (List<SyncConflict>) -> Map<String, ConflictChoice>?,
    ): SyncOutcome = withContext(Dispatchers.IO) {
        failingAs {
            val directory = library.workingDirectory()
            open(directory).use { git ->
                val repository = git.repository
                progress(SyncProgress(SyncStep.Committing))
                commitLocalChanges(git, directory)
                configureRemote(repository, remote.url)
                progress(SyncProgress(SyncStep.Downloading, 0f))
                val fetched = git.fetch().setRemote(REMOTE).setRemoveDeletedRefs(true).setCredentialsProvider(remote.credentials).setTimeout(TIMEOUT_SECONDS)
                    .setProgressMonitor(StepMonitor(SyncStep.Downloading, progress)).call()
                val branch = fetched.getAdvertisedRef(Constants.HEAD)?.target?.name?.removePrefix(Constants.R_HEADS) ?: repository.branch
                useBranch(git, branch)
                val remoteHead = repository.resolve("${Constants.R_REMOTES}$REMOTE/$branch")
                val localHead = repository.resolve(Constants.HEAD)
                if (remoteHead != null) {
                    progress(SyncProgress(SyncStep.Merging))
                    if (!integrate(git, directory, localHead, remoteHead, resolve)) return@failingAs SyncOutcome.Cancelled
                }
                val merged = repository.resolve(Constants.HEAD)
                removeDeletedScores(repository, directory, localHead, merged)
                val downloaded = changedScores(repository, localHead, merged)
                val uploaded = changedScores(repository, remoteHead, merged)
                if (merged != null && merged != remoteHead) push(git, branch, remote, progress)
                SyncOutcome.Completed(uploaded, downloaded)
            }
        }
    }

    private fun open(directory: File): Git {
        val git = if (File(directory, Constants.DOT_GIT).exists()) {
            Git.open(directory)
        } else {
            Git.init().setDirectory(directory).setInitialBranch(DEFAULT_BRANCH).call()
        }
        git.repository.config.apply {
            setBoolean(ConfigConstants.CONFIG_CORE_SECTION, null, ConfigConstants.CONFIG_KEY_FILEMODE, false)
            setString(ConfigConstants.CONFIG_CORE_SECTION, null, ConfigConstants.CONFIG_KEY_AUTOCRLF, false.toString())
            setInt(ConfigConstants.CONFIG_GC_SECTION, null, ConfigConstants.CONFIG_KEY_AUTO, 0)
            save()
        }
        ignoreDeviceFiles(directory)
        return git
    }

    private fun ignoreDeviceFiles(directory: File) {
        val file = File(directory, IGNORE_FILE)
        val existing = if (file.exists()) file.readLines() else emptyList()
        val missing = IgnoredPatterns.filterNot { it in existing }
        if (missing.isNotEmpty()) file.writeText((existing + missing).joinToString(System.lineSeparator(), postfix = System.lineSeparator()))
    }

    private fun commitLocalChanges(git: Git, directory: File) {
        directory.walkTopDown().onEnter { it.name != Constants.DOT_GIT }
            .filter { it.isDirectory && it != directory && it.list()?.isEmpty() == true }
            .forEach { File(it, KEEP_FILE).createNewFile() }
        git.add().addFilepattern(".").call()
        git.add().setUpdate(true).addFilepattern(".").call()
        if (git.status().call().hasUncommittedChanges()) commit(git, "Sync from ${deviceName()}")
    }

    private fun commit(git: Git, message: String) {
        val author = PersonIdent("citrinScore (${deviceName()})", AUTHOR_EMAIL)
        git.commit().setMessage(message).setAuthor(author).setCommitter(author).call()
    }

    private fun configureRemote(repository: Repository, url: String) {
        URIish(url)
        repository.config.apply {
            setString(ConfigConstants.CONFIG_REMOTE_SECTION, REMOTE, ConfigConstants.CONFIG_KEY_URL, url)
            setString(ConfigConstants.CONFIG_REMOTE_SECTION, REMOTE, ConfigConstants.CONFIG_FETCH_SECTION, "+${Constants.R_HEADS}*:${Constants.R_REMOTES}$REMOTE/*")
            save()
        }
    }

    private fun useBranch(git: Git, branch: String) {
        val repository = git.repository
        if (repository.branch == branch) return
        if (repository.resolve(Constants.HEAD) == null) {
            repository.updateRef(Constants.HEAD).link(Constants.R_HEADS + branch)
        } else {
            git.branchRename().setNewName(branch).call()
        }
    }

    private suspend fun integrate(
        git: Git,
        directory: File,
        localHead: ObjectId?,
        remoteHead: ObjectId,
        resolve: suspend (List<SyncConflict>) -> Map<String, ConflictChoice>?,
    ): Boolean {
        if (localHead == null) {
            git.repository.updateRef(Constants.HEAD).apply {
                setNewObjectId(remoteHead)
                update()
            }
            git.reset().setMode(ResetType.HARD).call()
            return true
        }
        val result = git.merge().include(remoteHead).setStrategy(MergeStrategy.RECURSIVE).setFastForward(FastForwardMode.FF)
            .setMessage("Merge synchronized changes").call()
        return when (result.mergeStatus) {
            MergeStatus.CONFLICTING -> resolveConflicts(git, directory, result.conflicts.keys, localHead, remoteHead, resolve)
            MergeStatus.FAILED, MergeStatus.ABORTED, MergeStatus.NOT_SUPPORTED, MergeStatus.CHECKOUT_CONFLICT ->
                throw SyncException(SyncFailure.Failed, "Merge failed: ${result.mergeStatus} ${result.failingPaths.orEmpty().keys}")
            else -> true
        }
    }

    private suspend fun resolveConflicts(
        git: Git,
        directory: File,
        paths: Collection<String>,
        localHead: ObjectId,
        remoteHead: ObjectId,
        resolve: suspend (List<SyncConflict>) -> Map<String, ConflictChoice>?,
    ): Boolean {
        val documents = paths.map(::documentOf).distinct().sorted()
        val choices = resolve(documents.map(::conflictOf))
        if (choices == null) {
            git.reset().setMode(ResetType.HARD).call()
            return false
        }
        documents.forEach { document ->
            val source = if (choices[document] == ConflictChoice.Remote) remoteHead else localHead
            restore(git, directory, document, source)
        }
        commit(git, "Merge synchronized changes")
        return true
    }

    private fun restore(git: Git, directory: File, document: String, source: ObjectId) {
        val repository = git.repository
        val tree = RevWalk(repository).use { it.parseCommit(source).tree }
        val exists = TreeWalk.forPath(repository, document, tree) != null
        git.rm().addFilepattern(document).call()
        if (exists) {
            git.checkout().setStartPoint(source.name).addPath(document).call()
        } else {
            File(directory, document).deleteRecursively()
        }
    }

    private fun removeDeletedScores(repository: Repository, directory: File, before: ObjectId?, after: ObjectId?) {
        if (before == null || after == null) return
        val tree = RevWalk(repository).use { it.parseCommit(after).tree }
        changedDocuments(repository, before, after)
            .filter { CitScore.isScoreDirectoryName(it.substringAfterLast(PATH_SEPARATOR)) && TreeWalk.forPath(repository, it, tree) == null }
            .forEach { File(directory, it).deleteRecursively() }
    }

    private fun changedScores(repository: Repository, before: ObjectId?, after: ObjectId?): Int =
        if (after == null || before == after) 0 else changedDocuments(repository, before, after).count { CitScore.isScoreDirectoryName(it.substringAfterLast(PATH_SEPARATOR)) }

    private fun changedDocuments(repository: Repository, before: ObjectId?, after: ObjectId): Set<String> =
        DiffFormatter(DisabledOutputStream.INSTANCE).use { formatter ->
            formatter.setRepository(repository)
            val trees = RevWalk(repository).use { walk -> listOf(before, after).map { id -> id?.let { walk.parseCommit(it).tree } } }
            formatter.scan(trees[0], trees[1])
                .flatMap { listOf(it.oldPath, it.newPath) }
                .filter { it != DiffEntry.DEV_NULL }
                .map(::documentOf)
                .toSet()
        }

    private fun push(git: Git, branch: String, remote: GitRemote, progress: (SyncProgress) -> Unit) {
        progress(SyncProgress(SyncStep.Uploading, 0f))
        val reference = Constants.R_HEADS + branch
        val rejected = git.push().setRemote(REMOTE).setRefSpecs(RefSpec("$reference:$reference")).setCredentialsProvider(remote.credentials).setTimeout(TIMEOUT_SECONDS)
            .setProgressMonitor(StepMonitor(SyncStep.Uploading, progress)).call()
            .flatMap { it.remoteUpdates }
            .filterNot { it.status in AcceptedPushes }
        rejected.firstOrNull()?.let { throw SyncException(SyncFailure.Rejected, "${it.status} ${it.message.orEmpty()}") }
    }

    private fun documentOf(path: String): String {
        val segments = path.split(PATH_SEPARATOR)
        val score = segments.indexOfFirst(CitScore::isScoreDirectoryName)
        return if (score < 0) path else segments.take(score + 1).joinToString(PATH_SEPARATOR)
    }

    private fun conflictOf(document: String): SyncConflict {
        val name = document.substringAfterLast(PATH_SEPARATOR)
        val folder = document.substringBeforeLast(PATH_SEPARATOR, "")
        return SyncConflict(document, folder, if (CitScore.isScoreDirectoryName(name)) CitScore.title(name) else name)
    }

    private val GitRemote.credentials: CredentialsProvider
        get() = UsernamePasswordCredentialsProvider(username.ifBlank { TOKEN_USERNAME }, token)

    private inline fun <T> failingAs(block: () -> T): T = try {
        block()
    } catch (e: SyncException) {
        throw e
    } catch (e: InvalidRemoteException) {
        throw SyncException(SyncFailure.Connection, e.message, e)
    } catch (e: TransportException) {
        throw SyncException(SyncFailure.Connection, e.message, e)
    } catch (e: GitAPIException) {
        throw SyncException(SyncFailure.Failed, e.message, e)
    } catch (e: IOException) {
        throw SyncException(SyncFailure.Failed, e.message, e)
    } catch (e: URISyntaxException) {
        throw SyncException(SyncFailure.Connection, e.message, e)
    }

    private class StepMonitor(private val step: SyncStep, private val progress: (SyncProgress) -> Unit) : ProgressMonitor {
        private var total = 0
        private var done = 0

        override fun start(totalTasks: Int) = Unit

        override fun beginTask(title: String?, totalWork: Int) {
            total = totalWork
            done = 0
            progress(SyncProgress(step, if (total > 0) 0f else null))
        }

        override fun update(completed: Int) {
            done += completed
            if (total > 0) progress(SyncProgress(step, (done.toFloat() / total).coerceAtMost(1f)))
        }

        override fun endTask() = Unit

        override fun isCancelled() = false

        override fun showDuration(enabled: Boolean) = Unit
    }
}
