package com.studyassistant.repository

import com.studyassistant.domain.model.*

// ── AI Repository ─────────────────────────────────────
interface AIRepository {
    suspend fun summarizeNote(content: String, language: AppLanguage): Result<String>
    suspend fun generateQuiz(content: String, numQuestions: Int, language: AppLanguage): Result<List<QuizQuestion>>
    suspend fun generateQuizTitle(content: String, language: AppLanguage): Result<String>
    suspend fun generateStudyPlan(weakAreas: List<WeakArea>, targetExam: String): Result<StudyPlan>
    suspend fun detectWeakAreas(quizHistory: List<Quiz>): Result<List<WeakArea>>
    suspend fun generateFlashcards(noteId: String, userId: String, content: String, language: AppLanguage): Result<List<Flashcard>>
    suspend fun chatWithNote(noteContent: String, history: List<ChatMessage>, userMessage: String): Result<String>
}
