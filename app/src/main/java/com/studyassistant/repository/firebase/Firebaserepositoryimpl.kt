package com.studyassistant.repository.firebase

import com.studyassistant.data.store.JsonPersistenceStore
import com.studyassistant.domain.model.Note
import com.studyassistant.domain.model.Quiz
import com.studyassistant.domain.model.StudyPlan
import com.studyassistant.domain.model.User
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
}