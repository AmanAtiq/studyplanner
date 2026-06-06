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

/**
 * UI State for Profile Screen
 * Manages user profile information, editing mode, and weak area detection
 */
data class ProfileUiState(
    // User Information
    val user: User? = null,
    val quizHistory: List<Quiz> = emptyList(),
    val weakAreas: List<WeakArea> = emptyList(),
    val badges: List<Badge> = emptyList(),

    // Preferences
    val selectedExam: String = "MDCAT",

    // Loading & Status
    val isSaving: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,

    // Edit Mode States
    val isEditMode: Boolean = false,
    val editName: String = "",
    val editBio: String = "",
    val selectedPhotoUri: String = "",
    val isUploadingPhoto: Boolean = false
)

/**
 * ViewModel for Profile Screen
 * Handles user profile management, editing, and weak area analysis
 */
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

    // ==================== User Data Loading ====================

    /**
     * Load user data from Firebase
     */
    private fun loadUser() {
        val user = firebaseRepository.getCurrentUser() ?: return
        _uiState.update { it.copy(
            user = user,
            selectedExam = user.targetExam.ifEmpty { "MDCAT" },
            editName = user.name,
            editBio = user.bio.ifEmpty { "" }
        ) }
        viewModelScope.launch {
            firebaseRepository.getQuizHistory(user.id).collect { history ->
                _uiState.update { it.copy(quizHistory = history) }
            }
        }
        viewModelScope.launch {
            val badges = firebaseRepository.getEarnedBadges(user.id)
            _uiState.update { it.copy(badges = badges) }
        }
    }

    /**
     * Load language preference from local storage
     */
    private fun loadPreferences() {
        // Language preference managed on the User object; no local UI toggle.
    }

    // ==================== Edit Mode Management ====================

    /**
     * Enable profile edit mode
     */
    fun enableEditMode() {
        val user = _uiState.value.user ?: return
        _uiState.update { it.copy(
            isEditMode = true,
            editName = user.name,
            editBio = user.bio.ifEmpty { "" }
        ) }
    }

    /**
     * Disable profile edit mode
     */
    fun disableEditMode() {
        _uiState.update { it.copy(isEditMode = false) }
    }

    /**
     * Update edited name (max 50 characters)
     */
    fun updateEditName(name: String) {
        if (name.length <= 50) {
            _uiState.update { it.copy(editName = name) }
        }
    }

    /**
     * Update edited bio (max 500 characters)
     */
    fun updateEditBio(bio: String) {
        if (bio.length <= 500) {
            _uiState.update { it.copy(editBio = bio) }
        }
    }

    // ==================== Profile Media Management ====================

    /**
     * Upload profile photo
     * Currently stores URI locally. In production, upload to Firebase Storage
     */
    fun uploadProfilePhoto(photoUri: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploadingPhoto = true) }
            // TODO: Upload to Firebase Storage and get download URL
            _uiState.update { it.copy(
                isUploadingPhoto = false,
                selectedPhotoUri = photoUri,
                successMessage = "Profile picture updated!"
            ) }
        }
    }

    // ==================== Profile Persistence ====================

    /**
     * Save profile edits (name, bio, photo, preferences)
     */
    fun saveProfileEdits() {
        val state = _uiState.value
        val user = state.user ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            val updatedUser = user.copy(
                name = state.editName,
                bio = state.editBio,
                photoUrl = state.selectedPhotoUri.ifEmpty { user.photoUrl },
                targetExam = state.selectedExam
            )

            firebaseRepository.updateUser(updatedUser).fold(
                onSuccess = {
                    _uiState.update { it.copy(
                        user = updatedUser,
                        isSaving = false,
                        isEditMode = false,
                        successMessage = "Profile updated successfully!"
                    )}
                },
                onFailure = { exception ->
                    _uiState.update { it.copy(
                        isSaving = false,
                        error = exception.message ?: "Failed to save profile"
                    ) }
                }
            )
        }
    }

    /**
     * Save user preferences (exam, language)
     */
    fun saveProfile() {
        val state = _uiState.value
        val user = state.user ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            val updatedUser = user.copy(
                targetExam = state.selectedExam
            )

            firebaseRepository.updateUser(updatedUser).fold(
                onSuccess = {
                    _uiState.update { it.copy(
                        user = updatedUser,
                        isSaving = false,
                        successMessage = "Profile saved!"
                    )}
                },
                onFailure = { exception ->
                    _uiState.update { it.copy(
                        isSaving = false,
                        error = exception.message
                    ) }
                }
            )
        }
    }

    /**
     * Sign out current user
     */
    fun signOut() {
        viewModelScope.launch {
            firebaseRepository.signOut()
        }
    }

    // ==================== Preference Updates ====================

    /**
     * Update target exam preference
     */
    fun onExamChange(exam: String) {
        _uiState.update { it.copy(selectedExam = exam) }
    }

    // ==================== Statistics & Analytics ====================

    /**
     * Calculate average score across all completed quizzes
     * @return Average score as integer percentage
     */
    fun averageScore(): Int {
        val history = _uiState.value.quizHistory
        if (history.isEmpty()) return 0

        return history.map { quiz ->
            if (quiz.questions.isEmpty()) 0
            else (quiz.score * 100) / quiz.questions.size
        }.average().toInt()
    }

    // ==================== Weak Area Detection ====================

    /**
     * Detect weak areas based on quiz history
     * Analyzes performance by topic and identifies areas below 70% accuracy
     * @return List of weak areas sorted by accuracy (lowest first)
     */
    fun detectWeakAreas(): List<WeakArea> {
        val quizzes = _uiState.value.quizHistory
        if (quizzes.isEmpty()) return emptyList()

        val topicPerformance = mutableMapOf<String, MutableList<Int>>()

        // Analyze each quiz to extract performance by topic
        quizzes.forEach { quiz ->
            val totalQuestions = quiz.questions.size
            if (totalQuestions > 0) {
                val correctAnswers = quiz.score
                val accuracyPercentage = (correctAnswers * 100) / totalQuestions

                // Use quiz.title (friendly) when available, otherwise fallback to noteId snippet
                val topic = quiz.title.takeIf { it.isNotBlank() } ?: quiz.noteId.take(20).ifEmpty { "General Topics" }

                topicPerformance.getOrPut(topic) { mutableListOf() }
                    .add(accuracyPercentage)
            }
        }

        // Calculate weak areas (topics with < 70% accuracy)
        val weakAreas = topicPerformance.toList().mapIndexed { index, (topic, scores) ->
            val avgAccuracy = (scores.average() / 100.0).toFloat()
            val attempts = scores.size

            WeakArea(
                id = "$index-${System.currentTimeMillis()}",
                userId = _uiState.value.user?.id ?: "",
                topic = topic,
                subject = "Quiz Topic",
                accuracy = avgAccuracy.coerceIn(0f, 1f),
                totalAttempts = attempts,
                suggestions = generateSuggestions(avgAccuracy)
            )
        }

        // Return only weak areas (< 70% accuracy), sorted by lowest first
        return weakAreas
            .filter { it.accuracy < 0.7f }
            .sortedBy { it.accuracy }
    }

    /**
     * Generate improvement suggestions based on accuracy level
     * @param accuracy Accuracy as decimal (0.0 - 1.0)
     * @return List of contextual suggestions
     */
    private fun generateSuggestions(accuracy: Float): List<String> {
        return when {
            accuracy < 0.4f -> listOf(
                "Review concepts thoroughly",
                "Practice with more examples",
                "Seek additional help or tutoring"
            )
            accuracy < 0.6f -> listOf(
                "Practice more questions",
                "Review key concepts",
                "Take another quiz to improve"
            )
            else -> listOf(
                "Keep practicing to improve",
                "Review borderline topics",
                "Focus on difficult questions"
            )
        }
    }

    // ==================== UI State Management ====================

    /**
     * Clear error and success messages
     */
    fun clearMessages() {
        _uiState.update { it.copy(error = null, successMessage = null) }
    }
}
