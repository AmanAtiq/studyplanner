package com.studyassistant.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyassistant.domain.model.*
import com.studyassistant.domain.usecase.SummarizeNoteUseCase
import com.studyassistant.domain.usecase.UploadNoteUseCase
import com.studyassistant.repository.FirebaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

data class UploadUiState(
    val title: String = "",
    val content: String = "",
    val summary: String = "",
    val selectedSubjectId: String = "",
    val subjects: List<com.studyassistant.domain.model.Subject> = emptyList(),
    val isUploading: Boolean = false,
    val isSummarizing: Boolean = false,
    val isSuccess: Boolean = false,
    val savedNoteId: String? = null,
    val selectedFileName: String? = null,
    val selectedFileBytesSize: Int = 0,
    val error: String? = null
)

@HiltViewModel
class UploadViewModel @Inject constructor(
    private val uploadNoteUseCase: UploadNoteUseCase,
    private val summarizeNoteUseCase: SummarizeNoteUseCase,
    private val firebaseRepository: FirebaseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UploadUiState())
    val uiState: StateFlow<UploadUiState> = _uiState.asStateFlow()

    init {
        loadSubjects()
    }

    private fun loadSubjects() {
        val userId = firebaseRepository.getCurrentUser()?.id ?: return
        viewModelScope.launch {
            firebaseRepository.getSubjects(userId).collect { subjects ->
                _uiState.update { it.copy(subjects = subjects) }
            }
        }
    }

    fun onTitleChange(v: String) = _uiState.update { it.copy(title = v) }
    fun onContentChange(v: String) = _uiState.update { it.copy(content = v) }
    fun onSubjectSelected(subjectId: String) = _uiState.update { it.copy(selectedSubjectId = subjectId) }
    fun setSelectedFile(name: String?, bytesSize: Int) = _uiState.update { it.copy(selectedFileName = name, selectedFileBytesSize = bytesSize) }

    /**
        * Upload note with optional file data, then summarize and save the note locally.
        * Quiz generation is now user-driven from the note detail screen.
     */
    fun uploadNote(fileBytes: ByteArray? = null, fileName: String? = null) {
        val state = _uiState.value
        if (state.title.isBlank() || state.content.isBlank()) {
            _uiState.update { it.copy(error = "Title and content are required") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isUploading = true, error = null) }
            val userId = firebaseRepository.getCurrentUser()?.id ?: run {
                _uiState.update { it.copy(error = "Not signed in", isUploading = false) }
                return@launch
            }

            Log.d("UploadVM", "uploadNote: starting upload for user=$userId title=${state.title}")
            val result = withTimeoutOrNull(60_000) {
                uploadNoteUseCase(
                    userId = userId,
                    title = state.title,
                    content = state.content,
                    fileBytes = fileBytes,
                    fileName = fileName,
                    fileType = if (fileName?.endsWith(".pdf", true) == true) FileType.PDF else FileType.TEXT,
                    subjectId = state.selectedSubjectId
                )
            }

            if (result == null) {
                Log.w("UploadVM", "uploadNote: upload timed out")
                _uiState.update { it.copy(isUploading = false, error = "Upload timed out. Check your network and try again.") }
                return@launch
            }

            Log.d("UploadVM", "uploadNote: upload completed, processing result")
            result.fold(
                onSuccess = { note ->
                    // Auto summarize after upload
                    _uiState.update { it.copy(isUploading = false, isSummarizing = true) }

                    // Summarize with timeout
                    val sumResult = withTimeoutOrNull(90_000) { summarizeNoteUseCase(note, AppLanguage.ENGLISH) }
                        ?: Result.failure(Exception("Summarization timed out"))

                    sumResult.fold(
                        onSuccess = { summarized ->
                            _uiState.update { it.copy(
                                summary = summarized.summary,
                                isSummarizing = false,
                                isSuccess = true,
                                savedNoteId = summarized.id
                            )}
                        },
                        onFailure = { e ->
                            _uiState.update { it.copy(isSummarizing = false) }
                            _uiState.update { it.copy(isSuccess = true, error = e.message, savedNoteId = note.id) }
                        }
                    )
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isUploading = false, error = e.message) }
                }
            )
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
    fun resetSuccess() = _uiState.update { it.copy(isSuccess = false, savedNoteId = null) }
    fun updateNoteContent(content: String) = _uiState.update { it.copy(content = content) }
}
