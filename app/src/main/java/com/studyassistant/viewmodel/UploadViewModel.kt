package com.studyassistant.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyassistant.domain.model.*
import com.studyassistant.domain.usecase.SummarizeNoteUseCase
import com.studyassistant.domain.usecase.UploadNoteUseCase
import com.studyassistant.repository.FirebaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UploadUiState(
    val title: String = "",
    val content: String = "",
    val summary: String = "",
    val isUploading: Boolean = false,
    val isSummarizing: Boolean = false,
    val isSuccess: Boolean = false,
    val selectedLanguage: AppLanguage = AppLanguage.ENGLISH,
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

    fun onTitleChange(v: String) = _uiState.update { it.copy(title = v) }
    fun onContentChange(v: String) = _uiState.update { it.copy(content = v) }
    fun onLanguageChange(lang: AppLanguage) = _uiState.update { it.copy(selectedLanguage = lang) }

    fun uploadNote() {
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
            val result = uploadNoteUseCase(
                userId = userId,
                title = state.title,
                content = state.content
            )
            result.fold(
                onSuccess = { note ->
                    // Auto summarize after upload
                    _uiState.update { it.copy(isUploading = false, isSummarizing = true) }
                    val sumResult = summarizeNoteUseCase(note, state.selectedLanguage)
                    sumResult.fold(
                        onSuccess = { summarized ->
                            _uiState.update { it.copy(
                                summary = summarized.summary,
                                isSummarizing = false,
                                isSuccess = true
                            )}
                        },
                        onFailure = { e ->
                            _uiState.update { it.copy(
                                isSummarizing = false,
                                isSuccess = true, // note saved, summary just failed
                                error = "Note saved, but summarization failed: ${e.message}"
                            )}
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
    fun resetSuccess() = _uiState.update { it.copy(isSuccess = false) }
}