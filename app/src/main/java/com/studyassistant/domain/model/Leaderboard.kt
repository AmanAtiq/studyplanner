package com.studyassistant.domain.model

data class LeaderboardEntry(
    val rank: Int = 0,
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val averageScore: Double = 0.0,
    val totalQuizzes: Int = 0,
    val isCurrentUser: Boolean = false
)

data class LeaderboardData(
    val entries: List<LeaderboardEntry> = emptyList(),
    val userRank: Int? = null,
    val userScore: Double = 0.0
)

enum class LeaderboardType {
    GLOBAL,
    SUBJECT_SPECIFIC
}
