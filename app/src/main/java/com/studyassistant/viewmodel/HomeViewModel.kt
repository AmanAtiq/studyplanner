package com.studyassistant.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyassistant.domain.model.*
import com.studyassistant.domain.usecase.DetectWeakAreasUseCase
import com.studyassistant.repository.FirebaseRepository
import com.studyassistant.repository.LocalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val notes: List<Note> = emptyList(),
    val weakAreas: List<WeakArea> = emptyList(),
    val currentUser: User? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    // Language selection removed from UI
    val pendingDeleteNoteId: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val firebaseRepository: FirebaseRepository,
    private val localRepository: LocalRepository,
    private val detectWeakAreasUseCase: DetectWeakAreasUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadCurrentUser()
        loadNotes()
        // loadLanguagePreference() removed
    }

    private fun loadCurrentUser() {
        val user = firebaseRepository.getCurrentUser()
        _uiState.update { it.copy(currentUser = user) }
        user?.let { loadWeakAreas(it.id) }
    }

    private fun loadNotes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            localRepository.getCachedNotes()
                .catch { e -> _uiState.update { it.copy(error = e.message, isLoading = false) } }
                .collect { notes ->
                    _uiState.update { it.copy(notes = notes, isLoading = false) }
                }
        }
    }

    private fun loadWeakAreas(userId: String) {
        viewModelScope.launch {
            val result = detectWeakAreasUseCase(userId)
            result.onSuccess { areas ->
                _uiState.update { it.copy(weakAreas = areas) }
            }
        }
    }

    // Language toggle removed from ViewModel

    fun deleteNote(noteId: String) {
        viewModelScope.launch {
            firebaseRepository.deleteNote(noteId)
            localRepository.deleteCachedNote(noteId)
        }
    }

    // Request deletion: shows confirmation in UI
    fun requestDeleteNote(noteId: String) {
        _uiState.update { it.copy(pendingDeleteNoteId = noteId) }
    }

    // Called when the user confirms deletion
    fun confirmDeleteNote() {
        val id = _uiState.value.pendingDeleteNoteId ?: return
        _uiState.update { it.copy(pendingDeleteNoteId = null) }
        deleteNote(id)
    }

    fun cancelDeleteNote() {
        _uiState.update { it.copy(pendingDeleteNoteId = null) }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}