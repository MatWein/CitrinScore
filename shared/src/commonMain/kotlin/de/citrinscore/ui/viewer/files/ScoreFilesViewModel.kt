package de.citrinscore.ui.viewer.files

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.citrinscore.library.LibraryRepository
import de.citrinscore.library.PartRef
import de.citrinscore.score.Attachment
import de.citrinscore.score.AttachmentKind
import de.citrinscore.util.attempt
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.name
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FilesState(val files: List<Attachment> = emptyList(), val failed: Boolean = false)

class ScoreFilesViewModel(private val part: StateFlow<PartRef?>, private val repository: LibraryRepository) : ViewModel() {

    private val _state = MutableStateFlow(FilesState())
    val state: StateFlow<FilesState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            part.filterNotNull().collectLatest { ref ->
                _state.update { it.copy(files = emptyList()) }
                attempt { repository.attachments(ref, AttachmentKind.File) }?.let { files -> _state.update { it.copy(files = files) } }
                    ?: _state.update { it.copy(failed = true) }
            }
        }
    }

    fun add(files: List<PlatformFile>) = perform { ref ->
        files.forEach { file ->
            val attachment = repository.addAttachment(ref, AttachmentKind.File, file, file.name)
            update(ref) { it.copy(files = it.files + attachment) }
        }
    }

    fun delete(attachment: Attachment) = perform { ref ->
        repository.deleteAttachment(ref, AttachmentKind.File, attachment)
        update(ref) { state -> state.copy(files = state.files - attachment) }
    }

    fun export(attachment: Attachment, target: PlatformFile) = perform { ref -> repository.exportAttachment(ref, attachment, target) }

    fun dismissFailure() = _state.update { it.copy(failed = false) }

    private fun update(ref: PartRef, transform: (FilesState) -> FilesState) {
        if (part.value == ref) _state.update(transform)
    }

    private fun perform(action: suspend (PartRef) -> Unit) {
        val ref = part.value ?: return
        viewModelScope.launch { attempt { action(ref) } ?: _state.update { it.copy(failed = true) } }
    }
}
