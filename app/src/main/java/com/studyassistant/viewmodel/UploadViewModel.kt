package com.studyassistant.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyassistant.domain.model.*
import com.studyassistant.domain.usecase.GenerateQuizUseCase
import com.studyassistant.domain.usecase.SummarizeNoteUseCase
import com.studyassistant.domain.usecase.UploadNoteUseCase
import com.studyassistant.repository.FirebaseRepository
import com.studyassistant.repository.LocalRepository
import com.studyassistant.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

data class UploadUiState(
    val title: String = "",
    val content: String = "",
    val summary: String = "",
    val isUploading: Boolean = false,
    val isSummarizing: Boolean = false,
    val isSuccess: Boolean = false,
    val generatedQuizNoteId: String? = null,
    val selectedFileName: String? = null,
    val selectedFileBytesSize: Int = 0,
    val error: String? = null
)

@HiltViewModel
class UploadViewModel @Inject constructor(
    private val uploadNoteUseCase: UploadNoteUseCase,
    private val summarizeNoteUseCase: SummarizeNoteUseCase,
    private val generateQuizUseCase: GenerateQuizUseCase,
    private val firebaseRepository: FirebaseRepository,
    private val localRepository: LocalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UploadUiState())
    val uiState: StateFlow<UploadUiState> = _uiState.asStateFlow()

    fun onTitleChange(v: String) = _uiState.update { it.copy(title = v) }
    fun onContentChange(v: String) = _uiState.update { it.copy(content = v) }
    fun setSelectedFile(name: String?, bytesSize: Int) = _uiState.update { it.copy(selectedFileName = name, selectedFileBytesSize = bytesSize) }

    /**
     * Upload note with optional file data. After saving the note, run summarization, then
     * generate a quiz from the note content (summary or original) and save the quiz.
     * When quiz generation completes, set generatedQuizId which UI can use to navigate to quiz screen.
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
                    fileType = if (fileName?.endsWith(".pdf", true) == true) FileType.PDF else FileType.TEXT
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
                                isSummarizing = false
                            )}

                            // Now generate quiz from the summarized content with timeout
                            val quizResult = withTimeoutOrNull(90_000) {
                                generateQuizUseCase(note.copy(summary = summarized.summary), numQuestions = Constants.DEFAULT_QUIZ_QUESTIONS, language = AppLanguage.ENGLISH)
                            } ?: Result.failure(Exception("Quiz generation timed out"))

                            quizResult.fold(
                                onSuccess = { quiz ->
                                    // Save quiz with timeout
                                    val saveRes = withTimeoutOrNull(30_000) { firebaseRepository.saveQuiz(quiz) }
                                        ?: Result.failure(Exception("Saving quiz timed out"))
                                    if (saveRes.isSuccess) {
                                        localRepository.cacheQuiz(quiz)
                                        _uiState.update { it.copy(isSuccess = true, generatedQuizNoteId = quiz.noteId) }
                                    } else {
                                        _uiState.update { it.copy(isSuccess = true, error = "Note saved and summarized, but saving quiz failed: ${saveRes.exceptionOrNull()?.message}") }
                                    }
                                },
                                onFailure = { e ->
                                    _uiState.update { it.copy(isSuccess = true, error = "Note saved, summary done, but quiz generation failed: ${e.message}") }
                                }
                            )
                        },
                        onFailure = { e ->
                            // Note saved but summary failed. Still attempt to generate quiz from original content
                            _uiState.update { it.copy(isSummarizing = false) }

                            val quizResult = withTimeoutOrNull(60_000) {
                                generateQuizUseCase(note, numQuestions = Constants.DEFAULT_QUIZ_QUESTIONS, language = AppLanguage.ENGLISH)
                            } ?: Result.failure(Exception("Quiz generation timed out"))

                            quizResult.fold(
                                onSuccess = { quiz ->
                                    val saveRes = withTimeoutOrNull(30_000) { firebaseRepository.saveQuiz(quiz) }
                                        ?: Result.failure(Exception("Saving quiz timed out"))
                                    if (saveRes.isSuccess) {
                                        localRepository.cacheQuiz(quiz)
                                        _uiState.update { it.copy(isSuccess = true, generatedQuizNoteId = quiz.noteId) }
                                    } else {
                                        _uiState.update { it.copy(isSuccess = true, error = "Note saved, but summarization and quiz saving failed: ${e.message}; ${saveRes.exceptionOrNull()?.message}") }
                                    }
                                },
                                onFailure = { e2 ->
                                    _uiState.update { it.copy(isSuccess = true, error = "Note saved, but summarization and quiz generation failed: ${e.message}; ${e2.message}") }
                                }
                            )
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
    fun resetSuccess() = _uiState.update { it.copy(isSuccess = false, generatedQuizNoteId = null) }
}