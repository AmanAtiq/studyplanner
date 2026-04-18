package com.studyassistant.repository

import com.studyassistant.domain.model.*
import kotlinx.coroutines.flow.Flow

// ── AI Repository ─────────────────────────────────────
interface AIRepository {
    suspend fun summarizeNote(content: String, language: AppLanguage): Result<String>
    suspend fun generateQuiz(content: String, numQuestions: Int, language: AppLanguage): Result<List<QuizQuestion>>
    suspend fun generateQuizTitle(content: String, language: AppLanguage): Result<String>
    suspend fun generateStudyPlan(weakAreas: List<WeakArea>, targetExam: String): Result<StudyPlan>
    suspend fun detectWeakAreas(quizHistory: List<Quiz>): Result<List<WeakArea>>
    suspend fun translateContent(content: String, targetLanguage: AppLanguage): Result<String>
}
