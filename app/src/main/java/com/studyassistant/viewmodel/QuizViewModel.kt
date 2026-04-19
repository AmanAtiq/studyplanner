package com.studyassistant.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyassistant.domain.model.*
import com.studyassistant.domain.usecase.GenerateQuizUseCase
import com.studyassistant.repository.FirebaseRepository
import com.studyassistant.repository.LocalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import javax.inject.Inject

data class QuizUiState(
    val quiz: Quiz? = null,
    val currentQuestionIndex: Int = 0,
    val selectedAnswerIndex: Int = -1,
    val isAnswerRevealed: Boolean = false,
    val isLoading: Boolean = false,
    val isFinished: Boolean = false,
    val isReviewMode: Boolean = false,
    val score: Int = 0,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val error: String? = null
)

@HiltViewModel
class QuizViewModel @Inject constructor(
    private val generateQuizUseCase: GenerateQuizUseCase,
    private val firebaseRepository: FirebaseRepository,
    private val localRepository: LocalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuizUiState())
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    fun loadQuiz(noteId: String, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    quiz = null,
                    currentQuestionIndex = 0,
                    selectedAnswerIndex = -1,
                    isAnswerRevealed = false,
                    isLoading = true,
                    isFinished = false,
                    isReviewMode = false,
                    score = 0,
                    error = null
                )
            }
            // Prefer language set on the user's profile; fall back to ENGLISH
            val lang = firebaseRepository.getCurrentUser()?.preferredLanguage ?: AppLanguage.ENGLISH
            val note = localRepository.getCachedNoteById(noteId)
                ?: run {
                    firebaseRepository.getNoteById(noteId).getOrNull()
                }
            if (note == null) {
                _uiState.update { it.copy(isLoading = false, error = "Note not found") }
                return@launch
            }

            // Check if a quiz already exists in local cache for this note
            val cachedQuizzes = localRepository.getCachedQuizzes().first()
            val existing = if (forceRefresh) null else cachedQuizzes.find { it.noteId == noteId }
            if (existing != null) {
                _uiState.update {
                    it.copy(
                        quiz = existing,
                        isLoading = false,
                        language = lang,
                        score = existing.score,
                        isReviewMode = existing.completed
                    )
                }
                return@launch
            }

            val result = generateQuizUseCase(note, language = lang)
            result.fold(
                onSuccess = { quiz ->
                    _uiState.update {
                        it.copy(
                            quiz = quiz,
                            isLoading = false,
                            language = lang,
                            score = 0,
                            isReviewMode = false
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
            )
        }
    }

    fun selectAnswer(index: Int) {
        val state = _uiState.value
        if (state.isAnswerRevealed || state.isFinished || state.isReviewMode) return
        _uiState.update { it.copy(selectedAnswerIndex = index) }
    }

    fun revealAnswer() {
        val state = _uiState.value
        if (state.selectedAnswerIndex == -1 || state.isReviewMode || state.isFinished) return
        val question = state.quiz?.questions?.getOrNull(state.currentQuestionIndex) ?: return
        val isCorrect = state.selectedAnswerIndex == question.correctAnswerIndex
        val updatedQuiz = state.quiz?.copy(
            questions = state.quiz.questions.mapIndexed { index, item ->
                if (index == state.currentQuestionIndex) item.copy(selectedAnswerIndex = state.selectedAnswerIndex) else item
            }
        )
        _uiState.update {
            it.copy(
                quiz = updatedQuiz,
                isAnswerRevealed = true,
                score = if (isCorrect) it.score + 1 else it.score
            )
        }
    }

    fun nextQuestion() {
        val state = _uiState.value
        if (state.isReviewMode || state.isFinished) return
        val total = state.quiz?.questions?.size ?: 0
        if (state.currentQuestionIndex + 1 >= total) {
            finishQuiz()
        } else {
            _uiState.update { it.copy(
                currentQuestionIndex = it.currentQuestionIndex + 1,
                selectedAnswerIndex = -1,
                isAnswerRevealed = false
            )}
        }
    }

    private fun finishQuiz() {
        viewModelScope.launch {
            val state = _uiState.value
            val quiz = state.quiz ?: return@launch
            val completed = quiz.copy(score = state.score, completed = true)
            firebaseRepository.saveQuiz(completed)
            localRepository.cacheQuiz(completed)
            _uiState.update { it.copy(quiz = completed, isFinished = true) }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}