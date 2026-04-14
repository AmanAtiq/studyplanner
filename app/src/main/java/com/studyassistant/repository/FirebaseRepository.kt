package com.studyassistant.repository

import com.studyassistant.domain.model.*
import kotlinx.coroutines.flow.Flow

// ── Firebase Repository ───────────────────────────────
interface FirebaseRepository {
    // Auth
    suspend fun signIn(email: String, password: String): Result<User>
    suspend fun signUp(name: String, email: String, password: String): Result<User>
    suspend fun signOut()
    fun getCurrentUser(): User?

    // Notes
    suspend fun saveNote(note: Note): Result<Note>
    suspend fun getNotes(userId: String): Flow<List<Note>>
    suspend fun getNoteById(noteId: String): Result<Note>
    suspend fun deleteNote(noteId: String): Result<Unit>
    suspend fun uploadFile(userId: String, fileBytes: ByteArray, fileName: String): Result<String>

    // Quizzes
    suspend fun saveQuiz(quiz: Quiz): Result<Quiz>
    suspend fun getQuizHistory(userId: String): Flow<List<Quiz>>

    // Study Plan
    suspend fun saveStudyPlan(plan: StudyPlan): Result<StudyPlan>
    suspend fun getStudyPlan(userId: String): Flow<StudyPlan?>

    // User
    suspend fun updateUser(user: User): Result<User>
}
