package com.studyassistant.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyassistant.domain.model.*
import com.studyassistant.domain.usecase.GetStudyPlanUseCase
import com.studyassistant.repository.FirebaseRepository
import com.studyassistant.repository.LocalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

data class PlannerUiState(
    val studyPlan: StudyPlan? = null,
    val weakAreas: List<WeakArea> = emptyList(),
    val isLoading: Boolean = false,
    val isGenerating: Boolean = false,
    val generatingStep: String = "",
    val error: String? = null,
    // The target exam used to generate or displayed for the planner (e.g., "MDCAT", "O Level")
    val targetExam: String? = null
)

@HiltViewModel
class PlannerViewModel @Inject constructor(
    private val getStudyPlanUseCase: GetStudyPlanUseCase,
    private val firebaseRepository: FirebaseRepository,
    private val localRepository: LocalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlannerUiState())
    val uiState: StateFlow<PlannerUiState> = _uiState.asStateFlow()

    init { loadCachedPlan(); loadUserExamPreference() }

    private fun loadCachedPlan() {
        viewModelScope.launch {
            val cached = localRepository.getCachedStudyPlan()
            if (cached != null) _uiState.update { it.copy(studyPlan = cached) }
        }
    }

    private fun loadUserExamPreference() {
        viewModelScope.launch {
            val currentUser = firebaseRepository.getCurrentUser()
            val exam = currentUser?.targetExam?.takeIf { it.isNotBlank() }
            if (exam != null) _uiState.update { it.copy(targetExam = exam) }
        }
    }

    fun generatePlan() {
        viewModelScope.launch {
            val userId = firebaseRepository.getCurrentUser()?.id ?: run {
                _uiState.update { it.copy(error = "Not signed in") }
                return@launch
            }
            _uiState.update { it.copy(isGenerating = true, generatingStep = "🤖 Generating AI study plan...", error = null) }
            Log.d("PlannerVM", "generatePlan: starting for user=$userId")

            try {
                // Determine target exam from user preferences (fall back to MDCAT)
                val currentUser = firebaseRepository.getCurrentUser()
                val preferredExam = currentUser?.targetExam?.takeIf { it.isNotBlank() } ?: "MDCAT"
                // store the exam we will use in UI state so the screen can show it
                _uiState.update { it.copy(targetExam = preferredExam) }

                // Call with 40-second timeout for AI response + buffer
                val result = withTimeoutOrNull(40_000) {
                    getStudyPlanUseCase(userId, preferredExam)
                }

                if (result == null) {
                    Log.w("PlannerVM", "generatePlan: operation timed out after 40s")
                    _uiState.update { it.copy(
                        isGenerating = false,
                        generatingStep = "",
                        error = "❌ Plan generation timed out. Using local default plan instead. This may happen if the AI service is slow."
                    ) }
                    return@launch
                }

                result.fold(
                    onSuccess = { plan ->
                        Log.d("PlannerVM", "generatePlan: plan generated successfully with ${plan.tasks.size} tasks")
                        _uiState.update { it.copy(
                            studyPlan = plan,
                            isGenerating = false,
                            generatingStep = ""
                        ) }
                    },
                    onFailure = { e ->
                        Log.e("PlannerVM", "generatePlan: failed with error: ${e.message}", e)
                        val errorMsg = when {
                            e.message?.contains("API key", ignoreCase = true) == true ->
                                "❌ API Key Error: ${e.message}"
                            e.message?.contains("network", ignoreCase = true) == true ->
                                "❌ Network Error: Check your internet connection"
                            e.message?.contains("timeout", ignoreCase = true) == true ->
                                "❌ Request Timed Out: The AI service is slow. Please try again."
                            e.message?.contains("authentication", ignoreCase = true) == true ->
                                "❌ Authentication Error: Please sign in again."
                            else -> "❌ Error: ${e.message ?: "Failed to generate study plan."}"
                        }
                        _uiState.update { it.copy(isGenerating = false, generatingStep = "", error = errorMsg) }
                    }
                )
            } catch (e: Exception) {
                Log.e("PlannerVM", "generatePlan: unexpected error: ${e.message}", e)
                _uiState.update { it.copy(isGenerating = false, generatingStep = "", error = "❌ Unexpected error: ${e.message}") }
            }
        }
    }

    fun toggleTaskCompletion(taskId: String) {
        val plan = _uiState.value.studyPlan ?: return
        val updated = plan.copy(
            tasks = plan.tasks.map { task ->
                if (task.id == taskId) task.copy(isCompleted = !task.isCompleted) else task
            }
        )
        _uiState.update { it.copy(studyPlan = updated) }
        viewModelScope.launch {
            localRepository.cacheStudyPlan(updated)
            firebaseRepository.saveStudyPlan(updated)
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}