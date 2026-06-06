package com.studyassistant.domain.model

import java.util.Date

data class QuizPerformanceStats(
    val subjectId: String = "",
    val subjectName: String = "",
    val totalQuizzes: Int = 0,
    val averageScore: Double = 0.0,
    val highestScore: Double = 0.0,
    val lowestScore: Double = 0.0,
    val improvementTrend: Double = 0.0 // positive = improving, negative = declining
)

data class PerformanceAnalyticsData(
    val userId: String = "",
    val overallAverageScore: Double = 0.0,
    val totalQuizzesTaken: Int = 0,
    val bestSubject: QuizPerformanceStats? = null,
    val worstSubject: QuizPerformanceStats? = null,
    val subjectStats: List<QuizPerformanceStats> = emptyList(),
    val recentScores: List<Double> = emptyList(),
    val lastUpdated: Date = Date()
)

data class PerformanceTrend(
    val date: Date = Date(),
    val score: Double = 0.0,
    val subjectId: String = ""
)
