package com.studyassistant.repository.firebase

import com.studyassistant.data.store.JsonPersistenceStore
import com.studyassistant.domain.model.*
import com.studyassistant.repository.FirebaseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class FirebaseRepositoryImpl @Inject constructor(
    private val store: JsonPersistenceStore
) : FirebaseRepository {

    override suspend fun signIn(email: String, password: String): Result<User> =
        store.signIn(email, password)

    override suspend fun signUp(name: String, email: String, password: String): Result<User> =
        store.signUp(name, email, password)

    override suspend fun signOut() {
        store.signOut()
    }

    override fun getCurrentUser(): User? =
        store.currentUser()

    override suspend fun saveNote(note: Note): Result<Note> =
        store.saveNote(note)

    override suspend fun getNotes(userId: String): Flow<List<Note>> =
        store.notesFlow().map { notes -> notes.filter { it.userId == userId } }

    override suspend fun getNoteById(noteId: String): Result<Note> =
        store.notesFlow().value.firstOrNull { it.id == noteId }?.let { Result.success(it) }
            ?: Result.failure(Exception("Note not found"))

    override suspend fun deleteNote(noteId: String): Result<Unit> =
        store.deleteNote(noteId)

    override suspend fun uploadFile(userId: String, fileBytes: ByteArray, fileName: String): Result<String> =
        store.uploadAttachment(userId, fileBytes, fileName)

    override suspend fun saveQuiz(quiz: Quiz): Result<Quiz> =
        store.saveQuiz(quiz)

    override suspend fun getQuizHistory(userId: String): Flow<List<Quiz>> =
        store.quizzesFlow().map { quizzes -> quizzes.filter { it.userId == userId && it.completed } }

    override suspend fun saveStudyPlan(plan: StudyPlan): Result<StudyPlan> =
        store.saveStudyPlan(plan)

    override suspend fun getStudyPlan(userId: String): Flow<StudyPlan?> =
        store.studyPlansFlow().map { plans -> plans.filter { it.userId == userId }.maxByOrNull { it.createdAt.time } }

    override suspend fun updateUser(user: User): Result<User> =
        store.updateUser(user)

    override suspend fun saveSubject(subject: Subject): Result<Subject> =
        store.saveSubject(subject)

    override suspend fun deleteSubject(subjectId: String): Result<Unit> =
        store.deleteSubject(subjectId)

    override suspend fun getSubjects(userId: String): Flow<List<Subject>> =
        store.subjectsFlow().map { list -> list.filter { it.userId == userId } }

    override suspend fun saveGradeEntry(entry: GradeEntry): Result<GradeEntry> =
        store.saveGrade(entry)

    override suspend fun getGradeHistory(userId: String): Flow<List<GradeEntry>> =
        store.gradesFlow().map { list -> list.filter { it.userId == userId }.sortedByDescending { it.createdAt } }

    override fun getAllGradeHistory(): Flow<List<GradeEntry>> =
        store.gradesFlow().map { list -> list.sortedByDescending { it.createdAt } }

    override fun getUsers(): Flow<List<User>> =
        store.authUsers.map { records -> records.map { it.user } }

    override suspend fun awardBadge(userId: String, badgeId: String): Result<Unit> =
        store.awardBadge(userId, badgeId)

    override suspend fun getEarnedBadges(userId: String): List<Badge> =
        store.getEarnedBadges(userId)

    override suspend fun getCompletedQuizCount(userId: String): Int =
        store.quizzesFlow().value.count { it.userId == userId && it.completed }

    override fun getStreak(userId: String): StreakData = store.getStreak(userId)

    override suspend fun updateStreak(userId: String): StreakData = store.updateStreak(userId)

    override suspend fun saveFlashcards(cards: List<Flashcard>): Result<List<Flashcard>> =
        store.saveFlashcards(cards)

    override fun getFlashcardsForNote(noteId: String): List<Flashcard> =
        store.getFlashcardsForNote(noteId)
}
