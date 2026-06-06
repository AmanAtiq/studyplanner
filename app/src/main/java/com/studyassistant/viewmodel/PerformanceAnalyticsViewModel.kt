package com.studyassistant.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyassistant.domain.model.PerformanceAnalyticsData
import com.studyassistant.domain.usecase.GetPerformanceAnalyticsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PerformanceAnalyticsUiState(
    val analytics: PerformanceAnalyticsData? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class PerformanceAnalyticsViewModel @Inject constructor(
    private val getPerformanceAnalyticsUseCase: GetPerformanceAnalyticsUseCase
) : ViewModel() {

    private val _uiState = kotlinx.coroutines.flow.MutableStateFlow(PerformanceAnalyticsUiState(isLoading = true))
    val uiState: StateFlow<PerformanceAnalyticsUiState> = _uiState.asStateFlow()

    init {
        loadAnalytics()
    }

    private fun loadAnalytics() {
        viewModelScope.launch {
            try {
                getPerformanceAnalyticsUseCase().collect { analytics ->
                    _uiState.value = PerformanceAnalyticsUiState(analytics = analytics, isLoading = false)
                }
            } catch (e: Exception) {
                _uiState.value = PerformanceAnalyticsUiState(
                    error = e.message ?: "Failed to load analytics",
                    isLoading = false
                )
            }
        }
    }

    fun retry() {
        loadAnalytics()
    }
}
