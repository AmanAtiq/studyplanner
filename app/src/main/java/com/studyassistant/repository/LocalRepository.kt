package com.studyassistant.repository

import com.studyassistant.domain.model.*
import kotlinx.coroutines.flow.Flow

// ── Local Repository ──────────────────────────────────
interface LocalRepository {
    // Notes cache
    suspend fun cacheNote(note: Note)
    suspend fun getCachedNotes(): Flow<List<Note>>
    suspend fun getCachedNoteById(noteId: String): Note?
    suspend fun deleteCachedNote(noteId: String)
    suspend fun getCachedNotesBySubject(subjectId: String): Flow<List<Note>>

    // Note-Subject assignment
    suspend fun assignSubjectToNote(noteId: String, subjectId: String)

    // Note-Badge assignment
    suspend fun assignBadgeToNote(noteId: String, badgeId: String)
    suspend fun removeBadgeFromNote(noteId: String, badgeId: String)
    suspend fun getBadgesForNote(noteId: String): Flow<List<String>>

    // Quiz cache
    suspend fun cacheQuiz(quiz: Quiz)
    suspend fun getCachedQuizzes(): Flow<List<Quiz>>

    // Study plan cache
    suspend fun cacheStudyPlan(plan: StudyPlan)
    suspend fun getCachedStudyPlan(): StudyPlan?

    // Preferences
    suspend fun saveLanguagePreference(language: AppLanguage)
    suspend fun getLanguagePreference(): AppLanguage
}
