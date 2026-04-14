package com.studyassistant.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyassistant.domain.model.*
import com.studyassistant.repository.FirebaseRepository
import com.studyassistant.repository.LocalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val user: User? = null,
    val quizHistory: List<Quiz> = emptyList(),
    val selectedLanguage: AppLanguage = AppLanguage.ENGLISH,
    val selectedExam: String = "MDCAT",
    val isSaving: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val firebaseRepository: FirebaseRepository,
    private val localRepository: LocalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadUser()
        loadPreferences()
    }

    private fun loadUser() {
        val user = firebaseRepository.getCurrentUser() ?: return
        _uiState.update { it.copy(user = user, selectedExam = user.targetExam.ifEmpty { "MDCAT" }) }
        viewModelScope.launch {
            firebaseRepository.getQuizHistory(user.id).collect { history ->
                _uiState.update { it.copy(quizHistory = history) }
            }
        }
    }

    private fun loadPreferences() {
        viewModelScope.launch {
            val lang = localRepository.getLanguagePreference()
            _uiState.update { it.copy(selectedLanguage = lang) }
        }
    }

    fun onExamChange(exam: String) = _uiState.update { it.copy(selectedExam = exam) }

    fun onLanguageChange(lang: AppLanguage) {
        _uiState.update { it.copy(selectedLanguage = lang) }
        viewModelScope.launch { localRepository.saveLanguagePreference(lang) }
    }

    fun saveProfile() {
        val state = _uiState.value
        val user = state.user ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val updated = user.copy(
                preferredLanguage = state.selectedLanguage,
                targetExam = state.selectedExam
            )
            firebaseRepository.updateUser(updated).fold(
                onSuccess = {
                    _uiState.update { it.copy(
                        user = updated,
                        isSaving = false,
                        successMessage = "Profile saved!"
                    )}
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isSaving = false, error = e.message) }
                }
            )
        }
    }

    fun signOut() {
        viewModelScope.launch { firebaseRepository.signOut() }
    }

    // Average score across all quizzes
    fun averageScore(): Int {
        val history = _uiState.value.quizHistory.filter { it.completed }
        if (history.isEmpty()) return 0
        return history.map { q ->
            if (q.questions.isEmpty()) 0
            else (q.score * 100) / q.questions.size
        }.average().toInt()
    }

    fun clearMessages() = _uiState.update { it.copy(error = null, successMessage = null) }
}