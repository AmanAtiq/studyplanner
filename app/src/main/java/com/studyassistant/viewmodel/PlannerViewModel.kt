package com.studyassistant.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyassistant.domain.model.*
import com.studyassistant.domain.usecase.GetStudyPlanUseCase
import com.studyassistant.repository.FirebaseRepository
import com.studyassistant.repository.LocalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlannerUiState(
    val studyPlan: StudyPlan? = null,
    val weakAreas: List<WeakArea> = emptyList(),
    val isLoading: Boolean = false,
    val isGenerating: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class PlannerViewModel @Inject constructor(
    private val getStudyPlanUseCase: GetStudyPlanUseCase,
    private val firebaseRepository: FirebaseRepository,
    private val localRepository: LocalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlannerUiState())
    val uiState: StateFlow<PlannerUiState> = _uiState.asStateFlow()

    init { loadCachedPlan() }

    private fun loadCachedPlan() {
        viewModelScope.launch {
            val cached = localRepository.getCachedStudyPlan()
            if (cached != null) _uiState.update { it.copy(studyPlan = cached) }
        }
    }

    fun generatePlan() {
        viewModelScope.launch {
            val userId = firebaseRepository.getCurrentUser()?.id ?: run {
                _uiState.update { it.copy(error = "Not signed in") }
                return@launch
            }
            _uiState.update { it.copy(isGenerating = true, error = null) }
            // Get target exam from user profile
            val result = getStudyPlanUseCase(userId, "MDCAT")
            result.fold(
                onSuccess = { plan ->
                    _uiState.update { it.copy(studyPlan = plan, isGenerating = false) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isGenerating = false, error = e.message) }
                }
            )
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