package com.studyassistant.domain.usecase

import com.studyassistant.domain.model.PerformanceAnalyticsData
import com.studyassistant.domain.model.QuizPerformanceStats
import com.studyassistant.repository.LocalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class GetPerformanceAnalyticsUseCase @Inject constructor(
    private val localRepository: LocalRepository
) {
    suspend operator fun invoke(): Flow<PerformanceAnalyticsData> {
        val notesFlow = localRepository.getCachedNotes()
        val quizzesFlow = localRepository.getCachedQuizzes()

        return combine(quizzesFlow, notesFlow) { quizzes, notes ->
            val notesById = notes.associateBy { it.id }
            val completedQuizzes = quizzes.filter { it.completed }

            if (completedQuizzes.isEmpty()) {
                PerformanceAnalyticsData()
            } else {
                val groupedBySubject = completedQuizzes.groupBy { quiz ->
                    val note = notesById[quiz.noteId]
                    note?.subjectId?.takeIf { it.isNotBlank() }
                        ?: note?.title?.takeIf { it.isNotBlank() }
                        ?: quiz.title.ifBlank { "Uncategorized" }
                }
                val subjectStats = groupedBySubject.map { (subjectId, quizzesInSubject) ->
                    val firstQuiz = quizzesInSubject.first()
                    val note = notesById[firstQuiz.noteId]
                    val scores = quizzesInSubject.map { it.score.toDouble() }
                    val avgScore = scores.average()
                    val trend = calculateTrend(quizzesInSubject)

                    QuizPerformanceStats(
                        subjectId = subjectId,
                        subjectName = note?.title?.takeIf { it.isNotBlank() }
                            ?: firstQuiz.title.ifBlank { "Uncategorized" },
                        totalQuizzes = quizzesInSubject.size,
                        averageScore = avgScore,
                        highestScore = scores.maxOrNull() ?: 0.0,
                        lowestScore = scores.minOrNull() ?: 0.0,
                        improvementTrend = trend
                    )
                }

                val allScores = completedQuizzes.map { it.score.toDouble() }
                val recentScores = completedQuizzes
                    .sortedByDescending { it.createdAt }
                    .take(10)
                    .map { it.score.toDouble() }

                PerformanceAnalyticsData(
                    overallAverageScore = allScores.average(),
                    totalQuizzesTaken = completedQuizzes.size,
                    bestSubject = subjectStats.maxByOrNull { it.averageScore },
                    worstSubject = subjectStats.minByOrNull { it.averageScore },
                    subjectStats = subjectStats.sortedByDescending { it.averageScore },
                    recentScores = recentScores
                )
            }
        }
    }

    private fun calculateTrend(quizzes: List<com.studyassistant.domain.model.Quiz>): Double {
        if (quizzes.size < 2) return 0.0
        val sorted = quizzes.sortedBy { it.createdAt }
        val firstHalf = sorted.take(sorted.size / 2).map { it.score }
        val secondHalf = sorted.drop(sorted.size / 2).map { it.score }
        
        val firstAvg = firstHalf.average()
        val secondAvg = secondHalf.average()
        return secondAvg - firstAvg
    }
}
