package com.studyassistant.domain.usecase

import com.studyassistant.domain.model.*
import com.studyassistant.repository.LocalRepository
import com.studyassistant.repository.FirebaseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

class GetPerformanceAnalyticsUseCase @Inject constructor(
    private val localRepository: LocalRepository,
    private val firebaseRepository: FirebaseRepository
) {
    suspend operator fun invoke(): Flow<PerformanceAnalyticsData> {
        val userId = firebaseRepository.getCurrentUser()?.id ?: ""
        
        // We combine local grades with firebase grades to ensure the analytics are always up to date
        // In a real app, you'd sync them, but here we'll just merge them for the view
        val localGradesFlow = localRepository.getCachedGrades()
        val remoteGradesFlow = firebaseRepository.getGradeHistory(userId).onStart { emit(emptyList()) }
        val notesFlow = localRepository.getCachedNotes()
        val subjectsFlow = localRepository.getCachedSubjects()

        return combine(localGradesFlow, remoteGradesFlow, notesFlow, subjectsFlow) { local, remote, notes, subjects ->
            // Merge grades and remove duplicates by ID
            val allGrades = (local + remote).distinctBy { it.id }
            
            if (allGrades.isEmpty()) {
                return@combine PerformanceAnalyticsData()
            }

            val subjectsById = subjects.associateBy { it.id }
            val notesById = notes.associateBy { it.id }

            // Group grades by subject
            val groupedBySubject = allGrades.groupBy { it.subjectId }
            
            val subjectStats = groupedBySubject.map { (subjectId, subjectGrades) ->
                val subjectName = subjectsById[subjectId]?.name 
                    ?: notesById[subjectGrades.firstOrNull()?.noteId]?.title
                    ?: subjectGrades.firstOrNull()?.noteTitle
                    ?: "Unknown Subject"
                
                val scores = subjectGrades.map { it.percentage.toDouble() }
                val avgScore = scores.average()
                
                QuizPerformanceStats(
                    subjectId = subjectId,
                    subjectName = subjectName,
                    totalQuizzes = subjectGrades.size,
                    averageScore = avgScore,
                    highestScore = scores.maxOrNull() ?: 0.0,
                    lowestScore = scores.minOrNull() ?: 0.0,
                    improvementTrend = calculateGradeTrend(subjectGrades)
                )
            }

            val allPercentages = allGrades.map { it.percentage.toDouble() }
            
            // Trend data: sorted by date
            val trendData = allGrades.sortedBy { it.createdAt }
                .map { grade ->
                    PerformanceTrend(
                        date = grade.createdAt,
                        score = grade.percentage.toDouble(),
                        subjectId = grade.subjectId
                    )
                }

            PerformanceAnalyticsData(
                userId = userId,
                overallAverageScore = allPercentages.average(),
                totalQuizzesTaken = allGrades.size,
                bestSubject = subjectStats.maxByOrNull { it.averageScore },
                worstSubject = subjectStats.minByOrNull { it.averageScore },
                subjectStats = subjectStats.sortedByDescending { it.averageScore },
                recentScores = allGrades.sortedByDescending { it.createdAt }.take(10).map { it.percentage.toDouble() },
                trends = trendData
            )
        }
    }

    private fun calculateGradeTrend(grades: List<GradeEntry>): Double {
        if (grades.size < 2) return 0.0
        val sorted = grades.sortedBy { it.createdAt }
        val firstHalf = sorted.take(sorted.size / 2).map { it.percentage }
        val secondHalf = sorted.drop(sorted.size / 2).map { it.percentage }
        
        return secondHalf.average() - firstHalf.average()
    }
}
