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

    // Subjects
    suspend fun saveSubject(subject: Subject): Result<Subject>
    suspend fun deleteSubject(subjectId: String): Result<Unit>
    suspend fun getSubjects(userId: String): Flow<List<Subject>>

    // Grades
    suspend fun saveGradeEntry(entry: GradeEntry): Result<GradeEntry>
    suspend fun getGradeHistory(userId: String): Flow<List<GradeEntry>>
    fun getAllGradeHistory(): Flow<List<GradeEntry>>
    fun getUsers(): Flow<List<User>>

    // Badges
    suspend fun awardBadge(userId: String, badgeId: String): Result<Unit>
    suspend fun getEarnedBadges(userId: String): List<Badge>
    suspend fun getCompletedQuizCount(userId: String): Int

    // Streak
    fun getStreak(userId: String): StreakData
    suspend fun updateStreak(userId: String): StreakData

    // Flashcards
    suspend fun saveFlashcards(cards: List<Flashcard>): Result<List<Flashcard>>
    fun getFlashcardsForNote(noteId: String): List<Flashcard>
}
