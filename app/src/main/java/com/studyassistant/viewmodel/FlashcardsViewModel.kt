package com.studyassistant.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyassistant.domain.model.AppLanguage
import com.studyassistant.domain.model.Flashcard
import com.studyassistant.repository.AIRepository
import com.studyassistant.repository.FirebaseRepository
import com.studyassistant.repository.LocalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FlashcardsUiState(
    val cards: List<Flashcard> = emptyList(),
    val currentIndex: Int = 0,
    val isFlipped: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val isFinished: Boolean = false,
    val isSaved: Boolean = false
)

@HiltViewModel
class FlashcardsViewModel @Inject constructor(
    private val aiRepository: AIRepository,
    private val firebaseRepository: FirebaseRepository,
    private val localRepository: LocalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FlashcardsUiState())
    val uiState: StateFlow<FlashcardsUiState> = _uiState.asStateFlow()

    fun loadFlashcards(noteId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, isFinished = false, currentIndex = 0, isFlipped = false) }

            val cached = firebaseRepository.getFlashcardsForNote(noteId)
            if (cached.isNotEmpty()) {
                _uiState.update { it.copy(cards = cached.shuffled(), isLoading = false, isSaved = true) }
                return@launch
            }

            val note = localRepository.getCachedNoteById(noteId)
                ?: firebaseRepository.getNoteById(noteId).getOrNull()

            if (note == null) {
                _uiState.update { it.copy(isLoading = false, error = "Note not found") }
                return@launch
            }

            val userId = firebaseRepository.getCurrentUser()?.id ?: ""
            val content = (note.summary + "\n\n" + note.originalContent).take(4000)
            val lang = firebaseRepository.getCurrentUser()?.preferredLanguage ?: AppLanguage.ENGLISH

            aiRepository.generateFlashcards(noteId, userId, content, lang).fold(
                onSuccess = { cards ->
                    // We don't save automatically now
                    _uiState.update { it.copy(cards = cards.shuffled(), isLoading = false, isSaved = false) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
            )
        }
    }

    fun saveFlashcards() {
        val cards = _uiState.value.cards
        if (cards.isEmpty()) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            firebaseRepository.saveFlashcards(cards).fold(
                onSuccess = {
                    _uiState.update { it.copy(isSaving = false, isSaved = true) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isSaving = false, error = "Failed to save: ${e.message}") }
                }
            )
        }
    }

    fun flip() = _uiState.update { it.copy(isFlipped = !it.isFlipped) }

    fun next() {
        val state = _uiState.value
        val nextIndex = state.currentIndex + 1
        if (nextIndex >= state.cards.size) {
            _uiState.update { it.copy(isFinished = true) }
        } else {
            _uiState.update { it.copy(currentIndex = nextIndex, isFlipped = false) }
        }
    }

    fun previous() {
        val state = _uiState.value
        if (state.currentIndex > 0) {
            _uiState.update { it.copy(currentIndex = state.currentIndex - 1, isFlipped = false) }
        }
    }

    fun restart() = _uiState.update { it.copy(currentIndex = 0, isFlipped = false, isFinished = false, cards = it.cards.shuffled()) }

    fun regenerate(noteId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, cards = emptyList(), isFinished = false, currentIndex = 0, isSaved = false) }
            val note = localRepository.getCachedNoteById(noteId)
                ?: firebaseRepository.getNoteById(noteId).getOrNull() ?: return@launch
            val userId = firebaseRepository.getCurrentUser()?.id ?: ""
            val content = (note.summary + "\n\n" + note.originalContent).take(4000)
            val lang = firebaseRepository.getCurrentUser()?.preferredLanguage ?: AppLanguage.ENGLISH
            aiRepository.generateFlashcards(noteId, userId, content, lang).fold(
                onSuccess = { cards ->
                    _uiState.update { it.copy(cards = cards.shuffled(), isLoading = false, isSaved = false) }
                },
                onFailure = { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
            )
        }
    }
}
