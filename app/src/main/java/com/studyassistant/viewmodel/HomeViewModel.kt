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
import java.util.UUID
import javax.inject.Inject
import com.studyassistant.domain.model.StreakData

data class HomeUiState(
    val notes: List<Note> = emptyList(),
    val allNotes: List<Note> = emptyList(),
    val weakAreas: List<WeakArea> = emptyList(),
    val currentUser: User? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val pendingDeleteNoteId: String? = null,
    val subjects: List<Subject> = emptyList(),
    val selectedSubjectId: String? = null,
    val showAddSubjectDialog: Boolean = false,
    val newSubjectName: String = "",
    val streak: StreakData = StreakData()
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
        loadSubjects()
        loadStreak()
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
                    val filtered = filterNotes(notes, _uiState.value.selectedSubjectId)
                    _uiState.update { it.copy(allNotes = notes, notes = filtered, isLoading = false) }
                }
        }
    }

    private fun loadSubjects() {
        val userId = firebaseRepository.getCurrentUser()?.id ?: return
        viewModelScope.launch {
            firebaseRepository.getSubjects(userId).collect { subjects ->
                _uiState.update { it.copy(subjects = subjects) }
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

    private fun loadStreak() {
        val userId = firebaseRepository.getCurrentUser()?.id ?: return
        val streak = firebaseRepository.getStreak(userId)
        _uiState.update { it.copy(streak = streak) }
    }

    private fun filterNotes(notes: List<Note>, subjectId: String?): List<Note> =
        if (subjectId == null) notes else notes.filter { it.subjectId == subjectId }

    fun selectSubject(id: String?) {
        val filtered = filterNotes(_uiState.value.allNotes, id)
        _uiState.update { it.copy(selectedSubjectId = id, notes = filtered) }
    }

    fun showAddSubjectDialog() = _uiState.update { it.copy(showAddSubjectDialog = true, newSubjectName = "") }
    fun hideAddSubjectDialog() = _uiState.update { it.copy(showAddSubjectDialog = false, newSubjectName = "") }
    fun setNewSubjectName(name: String) = _uiState.update { it.copy(newSubjectName = name) }

    fun addSubject() {
        val name = _uiState.value.newSubjectName.trim()
        val userId = _uiState.value.currentUser?.id ?: return
        if (name.isBlank()) return
        val existing = _uiState.value.subjects
        val colorHex = SUBJECT_COLORS[existing.size % SUBJECT_COLORS.size]
        val emoji = SUBJECT_EMOJIS[existing.size % SUBJECT_EMOJIS.size]
        viewModelScope.launch {
            val subject = Subject(id = UUID.randomUUID().toString(), userId = userId, name = name, colorHex = colorHex, emoji = emoji)
            firebaseRepository.saveSubject(subject)
            if (_uiState.value.subjects.isEmpty()) {
                firebaseRepository.awardBadge(userId, "first_subject")
            }
            hideAddSubjectDialog()
        }
    }

    fun deleteSubject(subjectId: String) {
        viewModelScope.launch {
            firebaseRepository.deleteSubject(subjectId)
            if (_uiState.value.selectedSubjectId == subjectId) selectSubject(null)
        }
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch {
            firebaseRepository.deleteNote(noteId)
            localRepository.deleteCachedNote(noteId)
        }
    }

    fun requestDeleteNote(noteId: String) {
        _uiState.update { it.copy(pendingDeleteNoteId = noteId) }
    }

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
