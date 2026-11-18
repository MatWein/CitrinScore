package de.citrinscore.ui.viewer.files

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.citrinscore.library.LibraryRepository
import de.citrinscore.score.Attachment
import de.citrinscore.score.AttachmentKind
import de.citrinscore.util.attempt
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.name
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FilesState(val files: List<Attachment> = emptyList(), val failed: Boolean = false)

class ScoreFilesViewModel(private val score: PlatformFile, private val repository: LibraryRepository) : ViewModel() {

    private val _state = MutableStateFlow(FilesState())
    val state: StateFlow<FilesState> = _state.asStateFlow()

    init {
        perform {
            val files = repository.attachments(score, AttachmentKind.File)
            _state.update { it.copy(files = files) }
        }
    }

    fun add(files: List<PlatformFile>) = perform {
        files.forEach { file ->
            val attachment = repository.addAttachment(score, AttachmentKind.File, file, file.name)
            _state.update { it.copy(files = it.files + attachment) }
        }
    }

    fun delete(attachment: Attachment) = perform {
        repository.deleteAttachment(score, AttachmentKind.File, attachment)
        _state.update { state -> state.copy(files = state.files - attachment) }
    }

    fun export(attachment: Attachment, target: PlatformFile) = perform { repository.exportAttachment(score, attachment, target) }

    fun dismissFailure() = _state.update { it.copy(failed = false) }

    private fun perform(action: suspend () -> Unit) {
        viewModelScope.launch { attempt { action() } ?: _state.update { it.copy(failed = true) } }
    }
}
