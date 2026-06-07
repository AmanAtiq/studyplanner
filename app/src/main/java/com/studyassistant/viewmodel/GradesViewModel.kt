package com.studyassistant.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyassistant.domain.model.GradeEntry
import com.studyassistant.repository.FirebaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GradesUiState(
    val grades: List<GradeEntry> = emptyList(),
    val isLoading: Boolean = false,
    val averageGrade: String = "-",
    val averagePct: Int = 0,
    val gradeCounts: Map<String, Int> = emptyMap()
)

@HiltViewModel
class GradesViewModel @Inject constructor(
    private val firebaseRepository: FirebaseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GradesUiState())
    val uiState: StateFlow<GradesUiState> = _uiState.asStateFlow()

    init { load() }

    private fun load() {
        val userId = firebaseRepository.getCurrentUser()?.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            firebaseRepository.getGradeHistory(userId).collect { grades ->
                val avgPct = if (grades.isEmpty()) 0 else grades.map { it.percentage }.average().toInt()
                val avgGrade = gradeLabel(avgPct)
                val counts = grades.groupBy { it.grade }.mapValues { it.value.size }
                _uiState.update { it.copy(grades = grades, isLoading = false, averageGrade = avgGrade, averagePct = avgPct, gradeCounts = counts) }
            }
        }
    }

    private fun gradeLabel(pct: Int) = when {
        pct >= 90 -> "A+"
        pct >= 80 -> "A"
        pct >= 70 -> "B"
        pct >= 60 -> "C"
        pct >= 50 -> "D"
        else -> "F"
    }

    fun clearHistory() {
        val userId = firebaseRepository.getCurrentUser()?.id ?: return
        viewModelScope.launch {
            val result = firebaseRepository.deleteGradeHistory(userId)
            result.fold(
                onSuccess = {
                    // Update UI to empty state
                    _uiState.update { it.copy(grades = emptyList(), averageGrade = "-", averagePct = 0, gradeCounts = emptyMap()) }
                },
                onFailure = {
                    // Keep existing state; optionally log the error
                }
            )
        }
    }
}
