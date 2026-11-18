package de.citrinscore.ui.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.citrinscore.settings.GitRemote
import de.citrinscore.sync.ConflictChoice
import de.citrinscore.sync.LibrarySync
import de.citrinscore.sync.SyncConflict
import de.citrinscore.sync.SyncException
import de.citrinscore.sync.SyncFailure
import de.citrinscore.sync.SyncOutcome
import de.citrinscore.sync.SyncProgress
import de.citrinscore.sync.SyncStep
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(SyncViewModel::class.java)

sealed interface SyncState {
    data object Idle : SyncState

    data class Running(val progress: SyncProgress) : SyncState

    data class Resolving(val conflicts: List<SyncConflict>, val choices: Map<String, ConflictChoice> = emptyMap()) : SyncState {
        val isComplete get() = conflicts.all { it.path in choices }
    }

    data class Finished(val outcome: SyncOutcome) : SyncState

    data class Failed(val failure: SyncFailure, val detail: String?) : SyncState
}

class SyncViewModel(private val library: PlatformFile, private val sync: LibrarySync) : ViewModel() {

    private val _state = MutableStateFlow<SyncState>(SyncState.Idle)
    val state: StateFlow<SyncState> = _state.asStateFlow()

    private var decision: CompletableDeferred<Map<String, ConflictChoice>?>? = null

    fun start(remote: GitRemote, onFinished: () -> Unit) {
        if (_state.value != SyncState.Idle) return
        _state.value = SyncState.Running(SyncProgress(SyncStep.Committing))
        viewModelScope.launch {
            _state.value = try {
                SyncState.Finished(sync.synchronize(library, remote, progress = { _state.value = SyncState.Running(it) }, resolve = ::ask))
            } catch (e: SyncException) {
                logger.warn("Synchronization failed", e)
                SyncState.Failed(e.failure, e.message)
            }
            onFinished()
        }
    }

    fun choose(conflict: SyncConflict, choice: ConflictChoice) {
        _state.update { state -> if (state is SyncState.Resolving) state.copy(choices = state.choices + (conflict.path to choice)) else state }
    }

    fun resume() {
        val resolving = _state.value as? SyncState.Resolving ?: return
        if (resolving.isComplete) decision?.complete(resolving.choices)
    }

    fun cancel() {
        decision?.complete(null)
    }

    fun dismiss() {
        if (_state.value is SyncState.Finished || _state.value is SyncState.Failed) _state.value = SyncState.Idle
    }

    private suspend fun ask(conflicts: List<SyncConflict>): Map<String, ConflictChoice>? {
        val pending = CompletableDeferred<Map<String, ConflictChoice>?>()
        decision = pending
        _state.value = SyncState.Resolving(conflicts)
        return pending.await().also {
            decision = null
            _state.value = SyncState.Running(SyncProgress(SyncStep.Merging))
        }
    }

    override fun onCleared() = cancel()
}
