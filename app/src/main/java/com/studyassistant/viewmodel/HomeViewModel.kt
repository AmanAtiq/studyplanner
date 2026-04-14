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
    val selectedLanguage: AppLanguage = AppLanguage.ENGLISH
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
        loadLanguagePreference()
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

    private fun loadLanguagePreference() {
        viewModelScope.launch {
            val lang = localRepository.getLanguagePreference()
            _uiState.update { it.copy(selectedLanguage = lang) }
        }
    }

    fun toggleLanguage() {
        viewModelScope.launch {
            val newLang = if (_uiState.value.selectedLanguage == AppLanguage.ENGLISH)
                AppLanguage.URDU else AppLanguage.ENGLISH
            localRepository.saveLanguagePreference(newLang)
            _uiState.update { it.copy(selectedLanguage = newLang) }
        }
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch {
            firebaseRepository.deleteNote(noteId)
            localRepository.deleteCachedNote(noteId)
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}