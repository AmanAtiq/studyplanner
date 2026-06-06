package com.studyassistant.repository.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.studyassistant.data.store.JsonPersistenceStore
import com.studyassistant.domain.model.*
import com.studyassistant.repository.FirebaseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject

class FirebaseRepositoryImpl @Inject constructor(
    private val store: JsonPersistenceStore,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : FirebaseRepository {

    // ── Auth ──────────────────────────────────────────────────────────────

    override suspend fun signUp(name: String, email: String, password: String): Result<User> {
        return try {
            // 1. Create in Firebase Auth
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user
                ?: return Result.failure(Exception("Sign up failed. Try again."))

            // 2. Build user object with Firebase UID
            val user = User(
                id = firebaseUser.uid,
                name = name,
                email = email,
                createdAt = Date()
            )

            // 3. Save to Firestore
            firestore.collection("users")
                .document(firebaseUser.uid)
                .set(user.toMap())
                .await()

            // 4. Also save locally (keeps rest of app working)
            store.signUp(name, email, password)

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signIn(email: String, password: String): Result<User> {
        return try {
            // 1. Sign in with Firebase Auth
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user
                ?: return Result.failure(Exception("Sign in failed. Try again."))

            // 2. Fetch user data from Firestore
            val doc = firestore.collection("users")
                .document(firebaseUser.uid)
                .get()
                .await()

            val user = if (doc.exists()) {
                doc.toUser()
            } else {
                // Firestore doc missing — create it
                val newUser = User(
                    id = firebaseUser.uid,
                    name = firebaseUser.displayName ?: "",
                    email = firebaseUser.email ?: "",
                    createdAt = Date()
                )
                firestore.collection("users")
                    .document(firebaseUser.uid)
                    .set(newUser.toMap())
                    .await()
                newUser
            }

            // 3. Also sign in locally
            store.signIn(email, password)

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(Exception("Invalid email or password."))
        }
    }

    override suspend fun signInWithGoogle(idToken: String): Result<User> {
        return try {
            // 1. Exchange Google token with Firebase Auth
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val firebaseUser = result.user
                ?: return Result.failure(Exception("Google Sign-In failed. Try again."))

            // 2. Check if user doc already exists in Firestore
            val doc = firestore.collection("users")
                .document(firebaseUser.uid)
                .get()
                .await()

            val user = if (doc.exists()) {
                // Returning user — fetch from Firestore
                doc.toUser()
            } else {
                // New Google user — create Firestore doc
                val newUser = User(
                    id = firebaseUser.uid,
                    name = firebaseUser.displayName ?: "Student",
                    email = firebaseUser.email ?: "",
                    photoUrl = firebaseUser.photoUrl?.toString() ?: "",
                    createdAt = Date()
                )
                firestore.collection("users")
                    .document(firebaseUser.uid)
                    .set(newUser.toMap())
                    .await()
                newUser
            }

            // 3. Sync to local store so rest of app works
            store.signInWithGoogle(idToken)

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(Exception("Google Sign-In failed: ${e.message}"))
        }
    }

    override suspend fun signOut() {
        auth.signOut()
        store.signOut()
    }

    override fun getCurrentUser(): User? {
        // Check Firebase Auth first
        val firebaseUser = auth.currentUser ?: return null
        // Fall back to local store for full user object
        return store.currentUser()
    }

    override suspend fun updateUser(user: User): Result<User> {
        return try {
            // Save to Firestore
            firestore.collection("users")
                .document(user.id)
                .set(user.toMap())
                .await()
            // Save locally too
            store.updateUser(user)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Everything else stays using local store for now ───────────────────

    override suspend fun saveNote(note: Note): Result<Note> = store.saveNote(note)
    override suspend fun getNotes(userId: String): Flow<List<Note>> = store.notesFlow().let {
        kotlinx.coroutines.flow.flow { store.notesFlow().collect { notes -> emit(notes.filter { it.userId == userId }) } }
    }
    override suspend fun getNoteById(noteId: String): Result<Note> =
        store.notesFlow().value.firstOrNull { it.id == noteId }?.let { Result.success(it) }
            ?: Result.failure(Exception("Note not found"))
    override suspend fun deleteNote(noteId: String): Result<Unit> = store.deleteNote(noteId)
    override suspend fun uploadFile(userId: String, fileBytes: ByteArray, fileName: String): Result<String> =
        store.uploadAttachment(userId, fileBytes, fileName)
    override suspend fun saveQuiz(quiz: Quiz): Result<Quiz> = store.saveQuiz(quiz)
    override suspend fun getQuizHistory(userId: String): Flow<List<Quiz>> =
        kotlinx.coroutines.flow.flow { store.quizzesFlow().collect { emit(it.filter { q -> q.userId == userId && q.completed }) } }
    override suspend fun saveStudyPlan(plan: StudyPlan): Result<StudyPlan> = store.saveStudyPlan(plan)
    override suspend fun getStudyPlan(userId: String): Flow<StudyPlan?> =
        kotlinx.coroutines.flow.flow { store.studyPlansFlow().collect { emit(it.filter { p -> p.userId == userId }.maxByOrNull { p -> p.createdAt.time }) } }
    override suspend fun saveSubject(subject: Subject): Result<Subject> = store.saveSubject(subject)
    override suspend fun deleteSubject(subjectId: String): Result<Unit> = store.deleteSubject(subjectId)
    override suspend fun getSubjects(userId: String): Flow<List<Subject>> =
        kotlinx.coroutines.flow.flow { store.subjectsFlow().collect { emit(it.filter { s -> s.userId == userId }) } }
    override suspend fun saveGradeEntry(entry: GradeEntry): Result<GradeEntry> = store.saveGrade(entry)
    override suspend fun getGradeHistory(userId: String): Flow<List<GradeEntry>> =
        kotlinx.coroutines.flow.flow { store.gradesFlow().collect { emit(it.filter { g -> g.userId == userId }.sortedByDescending { g -> g.createdAt }) } }
    override fun getAllGradeHistory(): Flow<List<GradeEntry>> =
        kotlinx.coroutines.flow.flow { store.gradesFlow().collect { emit(it.sortedByDescending { g -> g.createdAt }) } }
    override fun getUsers(): Flow<List<User>> =
        kotlinx.coroutines.flow.flow { store.authUsers.collect { emit(it.map { r -> r.user }) } }
    override suspend fun awardBadge(userId: String, badgeId: String): Result<Unit> = store.awardBadge(userId, badgeId)
    override suspend fun getEarnedBadges(userId: String): List<Badge> = store.getEarnedBadges(userId)
    override suspend fun getCompletedQuizCount(userId: String): Int =
        store.quizzesFlow().value.count { it.userId == userId && it.completed }
    override fun getStreak(userId: String): StreakData = store.getStreak(userId)
    override suspend fun updateStreak(userId: String): StreakData = store.updateStreak(userId)
    override suspend fun saveFlashcards(cards: List<Flashcard>): Result<List<Flashcard>> = store.saveFlashcards(cards)
    override fun getFlashcardsForNote(noteId: String): List<Flashcard> = store.getFlashcardsForNote(noteId)
}

// ── Extension functions to convert User ──────────────────────────────────────

fun User.toMap(): Map<String, Any> = mapOf(
    "id"                to id,
    "name"              to name,
    "email"             to email,
    "photoUrl"          to photoUrl,
    "bio"               to bio,
    "preferredLanguage" to preferredLanguage.name,
    "targetExam"        to targetExam,
    "createdAt"         to createdAt.time
)

fun com.google.firebase.firestore.DocumentSnapshot.toUser(): User = User(
    id                = getString("id") ?: id,
    name              = getString("name") ?: "",
    email             = getString("email") ?: "",
    photoUrl          = getString("photoUrl") ?: "",
    bio               = getString("bio") ?: "",
    preferredLanguage = try { AppLanguage.valueOf(getString("preferredLanguage") ?: "ENGLISH") } catch (e: Exception) { AppLanguage.ENGLISH },
    targetExam        = getString("targetExam") ?: "",
    createdAt         = Date(getLong("createdAt") ?: System.currentTimeMillis())
)