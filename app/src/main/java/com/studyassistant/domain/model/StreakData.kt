package com.studyassistant.domain.model

data class StreakData(
    val userId: String = "",
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastActiveDateStr: String = ""  // "yyyy-MM-dd"
)
