package com.studyassistant.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyassistant.domain.model.LeaderboardEntry
import com.studyassistant.repository.FirebaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LeaderboardUiState(
    val entries: List<LeaderboardEntry> = emptyList(),
    val userRank: Int? = null,
    val userScore: Double = 0.0,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class LeaderboardViewModel @Inject constructor(
    private val firebaseRepository: FirebaseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LeaderboardUiState(isLoading = true))
    val uiState: StateFlow<LeaderboardUiState> = _uiState.asStateFlow()

    init {
        loadLeaderboard()
    }

    private fun loadLeaderboard() {
        viewModelScope.launch {
            try {
                combine(
                    firebaseRepository.getAllGradeHistory(),
                    firebaseRepository.getUsers()
                ) { grades, users ->
                    val currentUser = firebaseRepository.getCurrentUser()
                    val usersById = users.associateBy { it.id }

                    val rankedEntries = grades
                        .filter { it.userId.isNotBlank() }
                        .groupBy { it.userId }
                        .map { (userId, userGrades) ->
                            val user = usersById[userId]
                            val averageScore = userGrades.map { it.percentage }.average()
                            LeaderboardEntry(
                                userId = userId,
                                userName = user?.name?.takeIf { it.isNotBlank() } ?: "User ${userId.take(6)}",
                                userEmail = user?.email ?: "",
                                averageScore = averageScore,
                                totalQuizzes = userGrades.size,
                                isCurrentUser = currentUser?.id == userId
                            )
                        }
                        .sortedWith(compareByDescending<LeaderboardEntry> { it.averageScore }
                            .thenByDescending { it.totalQuizzes })
                        .mapIndexed { index, entry -> entry.copy(rank = index + 1) }

                    if (rankedEntries.isEmpty()) {
                        _uiState.update { it.copy(entries = emptyList(), isLoading = false) }
                    } else {
                        val currentUserEntry = rankedEntries.firstOrNull { it.isCurrentUser }
                        _uiState.update { it.copy(
                            entries = rankedEntries,
                            userRank = currentUserEntry?.rank,
                            userScore = currentUserEntry?.averageScore ?: 0.0,
                            isLoading = false
                        )}
                    }
                }.collect { }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    error = e.message ?: "Failed to load leaderboard",
                    isLoading = false
                )}
            }
        }
    }

    fun retry() {
        loadLeaderboard()
    }
}
