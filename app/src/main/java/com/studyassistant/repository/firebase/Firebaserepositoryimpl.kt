package com.studyassistant.repository.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.studyassistant.domain.model.*
import com.studyassistant.repository.FirebaseRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

class FirebaseRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) : FirebaseRepository {

    // ── Auth ──────────────────────────────────────────
    override suspend fun signIn(email: String, password: String): Result<User> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: return Result.failure(Exception("Auth failed"))
            val doc = firestore.collection("users").document(uid).get().await()
            val user = doc.toObject(User::class.java) ?: User(id = uid, email = email)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signUp(name: String, email: String, password: String): Result<User> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: return Result.failure(Exception("Sign up failed"))
            val user = User(id = uid, name = name, email = email)
            firestore.collection("users").document(uid).set(user).await()
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signOut() { auth.signOut() }

    override fun getCurrentUser(): User? {
        val firebaseUser = auth.currentUser ?: return null
        return User(id = firebaseUser.uid, email = firebaseUser.email ?: "")
    }

    // ── Notes ─────────────────────────────────────────
    override suspend fun saveNote(note: Note): Result<Note> {
        return try {
            val id = note.id.ifEmpty { UUID.randomUUID().toString() }
            val toSave = note.copy(id = id)
            firestore.collection("notes").document(id).set(toSave).await()
            Result.success(toSave)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getNotes(userId: String): Flow<List<Note>> = callbackFlow {
        val listener = firestore.collection("notes")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val notes = snapshot?.documents?.mapNotNull { it.toObject(Note::class.java) } ?: emptyList()
                trySend(notes)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun getNoteById(noteId: String): Result<Note> {
        return try {
            val doc = firestore.collection("notes").document(noteId).get().await()
            val note = doc.toObject(Note::class.java) ?: return Result.failure(Exception("Note not found"))
            Result.success(note)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteNote(noteId: String): Result<Unit> {
        return try {
            firestore.collection("notes").document(noteId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadFile(userId: String, fileBytes: ByteArray, fileName: String): Result<String> {
        return try {
            val ref = storage.reference.child("notes/$userId/$fileName")
            ref.putBytes(fileBytes).await()
            val url = ref.downloadUrl.await().toString()
            Result.success(url)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Quizzes ───────────────────────────────────────
    override suspend fun saveQuiz(quiz: Quiz): Result<Quiz> {
        return try {
            val id = quiz.id.ifEmpty { UUID.randomUUID().toString() }
            val toSave = quiz.copy(id = id)
            firestore.collection("quizzes").document(id).set(toSave).await()
            Result.success(toSave)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getQuizHistory(userId: String): Flow<List<Quiz>> = callbackFlow {
        val listener = firestore.collection("quizzes")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val quizzes = snapshot?.documents?.mapNotNull { it.toObject(Quiz::class.java) } ?: emptyList()
                trySend(quizzes)
            }
        awaitClose { listener.remove() }
    }

    // ── Study Plan ────────────────────────────────────
    override suspend fun saveStudyPlan(plan: StudyPlan): Result<StudyPlan> {
        return try {
            val id = plan.id.ifEmpty { UUID.randomUUID().toString() }
            val toSave = plan.copy(id = id)
            firestore.collection("study_plans").document(plan.userId).set(toSave).await()
            Result.success(toSave)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getStudyPlan(userId: String): Flow<StudyPlan?> = callbackFlow {
        val listener = firestore.collection("study_plans").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.toObject(StudyPlan::class.java))
            }
        awaitClose { listener.remove() }
    }

    // ── User ──────────────────────────────────────────
    override suspend fun updateUser(user: User): Result<User> {
        return try {
            firestore.collection("users").document(user.id).set(user).await()
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}