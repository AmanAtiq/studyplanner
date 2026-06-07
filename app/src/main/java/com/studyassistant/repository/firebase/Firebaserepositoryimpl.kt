package com.studyassistant.repository.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.FirebaseFirestoreException
import com.studyassistant.data.store.JsonPersistenceStore
import com.studyassistant.domain.model.*
import com.studyassistant.repository.FirebaseRepository
import com.studyassistant.repository.LocalRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID
import javax.inject.Inject

class FirebaseRepositoryImpl @Inject constructor(
    private val store: JsonPersistenceStore,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val localRepository: LocalRepository
) : FirebaseRepository {

    // ── Auth ──────────────────────────────────────────────────────────────

    override suspend fun signUp(name: String, email: String, password: String): Result<User> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user
                ?: return Result.failure(Exception("Sign up failed. Try again."))

            val user = User(
                id = firebaseUser.uid,
                name = name,
                email = email,
                createdAt = Date()
            )

            firestore.collection("users")
                .document(firebaseUser.uid)
                .set(user.toFirebaseMap())
                .await()

            store.signUp(name, email, password)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signIn(email: String, password: String): Result<User> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user
                ?: return Result.failure(Exception("Sign in failed. Try again."))

            val doc = firestore.collection("users")
                .document(firebaseUser.uid)
                .get()
                .await()

            val user = if (doc.exists()) {
                doc.toUser()
            } else {
                val newUser = User(
                    id = firebaseUser.uid,
                    name = firebaseUser.displayName ?: "",
                    email = firebaseUser.email ?: "",
                    createdAt = Date()
                )
                firestore.collection("users")
                    .document(firebaseUser.uid)
                    .set(newUser.toFirebaseMap())
                    .await()
                newUser
            }

            store.signIn(email, password)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(Exception("Invalid email or password."))
        }
    }

    override suspend fun signInWithGoogle(idToken: String): Result<User> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val firebaseUser = result.user
                ?: return Result.failure(Exception("Google Sign-In failed. Try again."))

            val doc = firestore.collection("users")
                .document(firebaseUser.uid)
                .get()
                .await()

            val user = if (doc.exists()) {
                doc.toUser()
            } else {
                val newUser = User(
                    id = firebaseUser.uid,
                    name = firebaseUser.displayName ?: "Student",
                    email = firebaseUser.email ?: "",
                    photoUrl = firebaseUser.photoUrl?.toString() ?: "",
                    createdAt = Date()
                )
                firestore.collection("users")
                    .document(firebaseUser.uid)
                    .set(newUser.toFirebaseMap())
                    .await()
                newUser
            }

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
        if (auth.currentUser == null) return null
        return store.currentUser()
    }

    override suspend fun updateUser(user: User): Result<User> {
        return try {
            firestore.collection("users")
                .document(user.id)
                .set(user.toFirebaseMap(), SetOptions.merge())
                .await()
            store.updateUser(user)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Notes ─────────────────────────────────────────────────────────────

    override suspend fun saveNote(note: Note): Result<Note> = try {
        val noteId = note.id.ifBlank { UUID.randomUUID().toString() }
        val noteToSave = note.copy(id = noteId)
        
        firestore.collection("users").document(noteToSave.userId)
            .collection("notes").document(noteId)
            .set(noteToSave.toFirebaseMap())
            .await()
        store.saveNote(noteToSave)
        Result.success(noteToSave)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getNotes(userId: String): Flow<List<Note>> = callbackFlow {
        val listener = firestore.collection("users").document(userId)
            .collection("notes")
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val notes = snapshot?.documents?.mapNotNull { it.toNote() } ?: emptyList()
                trySend(notes)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun getNoteById(noteId: String): Result<Note> = try {
        val userId = auth.currentUser?.uid ?: throw Exception("Not logged in")
        val doc = firestore.collection("users").document(userId)
            .collection("notes").document(noteId)
            .get().await()

        doc.toNote()?.let { Result.success(it) } ?: Result.failure(Exception("Note not found"))
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun deleteNote(noteId: String): Result<Unit> = try {
        val userId = auth.currentUser?.uid ?: throw Exception("Not logged in")
        firestore.collection("users").document(userId)
            .collection("notes").document(noteId)
            .delete().await()
        store.deleteNote(noteId)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun uploadFile(userId: String, fileBytes: ByteArray, fileName: String): Result<String> =
        store.uploadAttachment(userId, fileBytes, fileName)

    // ── Quizzes ───────────────────────────────────────────────────────────

    override suspend fun saveQuiz(quiz: Quiz): Result<Quiz> = try {
        val quizId = quiz.id.ifBlank { UUID.randomUUID().toString() }
        val quizToSave = quiz.copy(id = quizId)

        firestore.collection("users").document(quizToSave.userId)
            .collection("quizzes").document(quizId)
            .set(quizToSave.toFirebaseMap())
            .await()
        store.saveQuiz(quizToSave)
        Result.success(quizToSave)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getQuizHistory(userId: String): Flow<List<Quiz>> = callbackFlow {
        val collRef = firestore.collection("users").document(userId).collection("quizzes")

        val listener = collRef
            .whereEqualTo("completed", true)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                try {
                    if (error != null) {
                        val isIndexError = (error is FirebaseFirestoreException && error.code == FirebaseFirestoreException.Code.FAILED_PRECONDITION)
                                || error.message?.contains("requires an index", ignoreCase = true) == true

                        if (isIndexError) {
                            collRef
                                .whereEqualTo("completed", true)
                                .get()
                                .addOnSuccessListener { snap ->
                                    val quizzes = snap.documents
                                        .mapNotNull { it.toQuiz() }
                                        .sortedByDescending { it.createdAt.time }
                                    trySend(quizzes)
                                }
                                .addOnFailureListener { trySend(emptyList()) }
                            return@addSnapshotListener
                        }

                        close(error)
                        return@addSnapshotListener
                    }

                    val quizzes = snapshot?.documents?.mapNotNull { it.toQuiz() } ?: emptyList()
                    trySend(quizzes)
                } catch (t: Throwable) {
                    close(t)
                }
            }

        awaitClose { listener.remove() }
    }

    // ── Study Plan ────────────────────────────────────────────────────────

    override suspend fun saveStudyPlan(plan: StudyPlan): Result<StudyPlan> = try {
        val planId = plan.id.ifBlank { UUID.randomUUID().toString() }
        val planToSave = plan.copy(id = planId)

        firestore.collection("users").document(planToSave.userId)
            .collection("studyPlans").document(planId)
            .set(planToSave.toFirebaseMap())
            .await()
        store.saveStudyPlan(planToSave)
        Result.success(planToSave)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getStudyPlan(userId: String): Flow<StudyPlan?> = callbackFlow {
        val listener = firestore.collection("users").document(userId)
            .collection("studyPlans")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val plan = snapshot?.documents?.firstOrNull()?.toStudyPlan()
                trySend(plan)
            }
        awaitClose { listener.remove() }
    }

    // ── Subjects ──────────────────────────────────────────────────────────

    override suspend fun saveSubject(subject: Subject): Result<Subject> = try {
        val subjectId = subject.id.ifBlank { UUID.randomUUID().toString() }
        val subjectToSave = subject.copy(id = subjectId)

        firestore.collection("users").document(subjectToSave.userId)
            .collection("subjects").document(subjectId)
            .set(subjectToSave.toFirebaseMap())
            .await()
        store.saveSubject(subjectToSave)
        Result.success(subjectToSave)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun deleteSubject(subjectId: String): Result<Unit> = try {
        val userId = auth.currentUser?.uid ?: throw Exception("Not logged in")
        firestore.collection("users").document(userId)
            .collection("subjects").document(subjectId)
            .delete().await()
        store.deleteSubject(subjectId)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getSubjects(userId: String): Flow<List<Subject>> = callbackFlow {
        val listener = firestore.collection("users").document(userId)
            .collection("subjects")
            .orderBy("name", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val subjects = snapshot?.documents?.mapNotNull { it.toSubject() } ?: emptyList()
                trySend(subjects)
            }
        awaitClose { listener.remove() }
    }

    // ── Grades ────────────────────────────────────────────────────────────

    override suspend fun saveGradeEntry(entry: GradeEntry): Result<GradeEntry> = try {
        val entryId = entry.id.ifBlank { UUID.randomUUID().toString() }
        val entryToSave = entry.copy(id = entryId)

        firestore.collection("users").document(entryToSave.userId)
            .collection("grades").document(entryId)
            .set(entryToSave.toFirebaseMap())
            .await()
        store.saveGrade(entryToSave)
        Result.success(entryToSave)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getGradeHistory(userId: String): Flow<List<GradeEntry>> = callbackFlow {
        val listener = firestore.collection("users").document(userId)
            .collection("grades")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    val isIndexError = (error is FirebaseFirestoreException && error.code == FirebaseFirestoreException.Code.FAILED_PRECONDITION)
                            || error.message?.contains("requires an index", ignoreCase = true) == true
                    
                    if (isIndexError) {
                        firestore.collection("users").document(userId).collection("grades")
                            .get()
                            .addOnSuccessListener { snap ->
                                val grades = snap.documents.mapNotNull { it.toGradeEntry() }
                                    .sortedByDescending { it.createdAt.time }
                                trySend(grades)
                            }.addOnFailureListener { close(error) }
                        return@addSnapshotListener
                    }
                    close(error)
                    return@addSnapshotListener
                }
                val grades = snapshot?.documents?.mapNotNull { it.toGradeEntry() } ?: emptyList()
                trySend(grades)
            }
        awaitClose { listener.remove() }
    }

    override fun getAllGradeHistory(): Flow<List<GradeEntry>> = callbackFlow {
        val collRef = firestore.collectionGroup("grades")
        val listener = collRef
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    val isIndexError = (error is FirebaseFirestoreException && error.code == FirebaseFirestoreException.Code.FAILED_PRECONDITION)
                            || error.message?.contains("requires an index", ignoreCase = true) == true
                    
                    if (isIndexError) {
                        collRef.get().addOnSuccessListener { snap ->
                            val grades = snap.documents.mapNotNull { it.toGradeEntry() }
                                .sortedByDescending { it.createdAt.time }
                            trySend(grades)
                        }.addOnFailureListener { close(error) }
                        return@addSnapshotListener
                    }
                    close(error)
                    return@addSnapshotListener
                }
                val grades = snapshot?.documents?.mapNotNull { it.toGradeEntry() } ?: emptyList()
                trySend(grades)
            }
        awaitClose { listener.remove() }
    }

    override fun getUsers(): Flow<List<User>> = callbackFlow {
        val listener = firestore.collection("users")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val users = snapshot?.documents?.mapNotNull { it.toUser() } ?: emptyList()
                trySend(users)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun deleteGradeHistory(userId: String): Result<Unit> = try {
        val snapshot = firestore.collection("users").document(userId).collection("grades").get().await()
        val batch = firestore.batch()
        snapshot.documents.forEach { batch.delete(it.reference) }
        batch.commit().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ── Badges ────────────────────────────────────────────────────────────

    override suspend fun awardBadge(userId: String, badgeId: String): Result<Unit> = try {
        val badgeData = mapOf(
            "badgeId" to badgeId,
            "awardedAt" to System.currentTimeMillis()
        )
        firestore.collection("users").document(userId)
            .collection("badges").document(badgeId)
            .set(badgeData)
            .await()
        
        val badge = BadgeDefinitions.byId(badgeId)?.copy(earnedAt = Date())
        badge?.let { localRepository.cacheBadge(it) }
        
        store.awardBadge(userId, badgeId)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getEarnedBadges(userId: String): List<Badge> = try {
        firestore.collection("users").document(userId)
            .collection("badges").get().await()
        store.getEarnedBadges(userId)
    } catch (e: Exception) {
        emptyList()
    }

    override suspend fun getCompletedQuizCount(userId: String): Int = try {
        val snapshot = firestore.collection("users").document(userId)
            .collection("quizzes")
            .whereEqualTo("completed", true)
            .get().await()
        snapshot.size()
    } catch (e: Exception) {
        0
    }

    // ── Streak ────────────────────────────────────────────────────────────

    override fun getStreak(userId: String): StreakData = store.getStreak(userId)

    override suspend fun updateStreak(userId: String): StreakData = try {
        val streak = store.updateStreak(userId)
        firestore.collection("users").document(userId)
            .collection("stats").document("streak")
            .set(mapOf("currentStreak" to streak.currentStreak, "lastActiveDate" to streak.lastActiveDateStr))
            .await()
        streak
    } catch (e: Exception) {
        store.getStreak(userId)
    }

    // ── Flashcards ────────────────────────────────────────────────────────

    override suspend fun saveFlashcards(cards: List<Flashcard>): Result<List<Flashcard>> = try {
        val userId = auth.currentUser?.uid ?: throw Exception("Not logged in")
        val batch = firestore.batch()
        cards.forEach { card ->
            val id = card.id.ifBlank { UUID.randomUUID().toString() }
            val cardToSave = card.copy(id = id)
            val docRef = firestore.collection("users").document(userId)
                .collection("flashcards").document(id)
            batch.set(docRef, cardToSave.toFirebaseMap())
        }
        batch.commit().await()
        store.saveFlashcards(cards)
        Result.success(cards)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override fun getFlashcardsForNote(noteId: String): List<Flashcard> = store.getFlashcardsForNote(noteId)

    // ── Chat ──────────────────────────────────────────────────────────────

    override suspend fun saveChatMessage(userId: String, noteId: String, message: ChatMessage): Result<Unit> {
        return try {
            val id = message.id.ifBlank { UUID.randomUUID().toString() }
            val msgToSave = message.copy(id = id)
            firestore.collection("chats")
                .document(userId)
                .collection(noteId)
                .document(id)
                .set(msgToSave.toFirebaseMap())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getChatMessages(userId: String, noteId: String): Flow<List<ChatMessage>> = callbackFlow {
        val listener = firestore.collection("chats")
            .document(userId)
            .collection(noteId)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents?.mapNotNull { it.toChatMessage() } ?: emptyList()
                trySend(messages)
            }
        awaitClose { listener.remove() }
    }

    // ── Leaderboard ───────────────────────────────────────────────────────

    override fun getLeaderboard(): Flow<List<LeaderboardEntry>> = callbackFlow {
        val listener = firestore.collection("users")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val entries = snapshot?.documents?.mapIndexed { index, doc ->
                    LeaderboardEntry(
                        rank = index + 1,
                        userId = doc.id,
                        userName = doc.getString("name") ?: "",
                        userEmail = doc.getString("email") ?: "",
                        averageScore = 0.0,
                        totalQuizzes = 0
                    )
                } ?: emptyList()
                trySend(entries)
            }
        awaitClose { listener.remove() }
    }

    // ── Study Groups ──────────────────────────────────────────────────────

    override suspend fun saveStudyGroup(group: StudyGroup): Result<Unit> = try {
        val id = group.id.ifBlank { UUID.randomUUID().toString() }
        val groupToSave = group.copy(id = id)
        firestore.collection("studyGroups").document(id)
            .set(groupToSave.toFirebaseMap())
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun joinStudyGroup(groupId: String, member: StudyGroupMember): Result<Unit> = try {
        firestore.collection("studyGroups").document(groupId)
            .collection("members").document(member.userId)
            .set(member.toFirebaseMap())
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun sendGroupMessage(groupId: String, message: GroupMessage): Result<Unit> = try {
        val id = message.id.ifBlank { UUID.randomUUID().toString() }
        val msgToSave = message.copy(id = id)
        firestore.collection("studyGroups").document(groupId)
            .collection("messages").document(id)
            .set(msgToSave.toFirebaseMap())
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override fun getActiveStudyGroups(): Flow<List<StudyGroup>> = callbackFlow {
        val listener = firestore.collection("studyGroups")
            .whereEqualTo("isActive", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val groups = snapshot?.documents?.mapNotNull { it.toStudyGroup() } ?: emptyList()
                trySend(groups)
            }
        awaitClose { listener.remove() }
    }

    override fun getGroupMessages(groupId: String): Flow<List<GroupMessage>> = callbackFlow {
        val listener = firestore.collection("studyGroups").document(groupId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents?.mapNotNull { it.toGroupMessage() } ?: emptyList()
                trySend(messages)
            }
        awaitClose { listener.remove() }
    }

    override fun getGroupMembers(groupId: String): Flow<List<StudyGroupMember>> = callbackFlow {
        val listener = firestore.collection("studyGroups").document(groupId)
            .collection("members")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val members = snapshot?.documents?.mapNotNull { it.toStudyGroupMember() } ?: emptyList()
                trySend(members)
            }
        awaitClose { listener.remove() }
    }

    override fun getMemberGroups(userId: String): Flow<List<StudyGroup>> = callbackFlow {
        val listener = firestore.collectionGroup("members")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(emptyList())
            }
        awaitClose { listener.remove() }
    }
}

// ── Additional Extension functions ──────────────────────────────────────────

fun User.toFirebaseMap(): Map<String, Any?> = mapOf(
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

fun Note.toFirebaseMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "userId" to userId,
    "title" to title,
    "originalContent" to originalContent,
    "summary" to summary,
    "language" to language.name,
    "fileUrl" to fileUrl,
    "fileType" to fileType.name,
    "subjectId" to subjectId,
    "createdAt" to createdAt.time,
    "updatedAt" to updatedAt.time
)

fun com.google.firebase.firestore.DocumentSnapshot.toNote(): Note? = try {
    Note(
        id = getString("id") ?: id,
        userId = getString("userId") ?: "",
        title = getString("title") ?: "",
        originalContent = getString("originalContent") ?: "",
        summary = getString("summary") ?: "",
        language = AppLanguage.valueOf(getString("language") ?: "ENGLISH"),
        fileUrl = getString("fileUrl") ?: "",
        fileType = FileType.valueOf(getString("fileType") ?: "TEXT"),
        subjectId = getString("subjectId") ?: "",
        createdAt = Date(getLong("createdAt") ?: System.currentTimeMillis()),
        updatedAt = Date(getLong("updatedAt") ?: System.currentTimeMillis())
    )
} catch (e: Exception) { null }

fun Quiz.toFirebaseMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "noteId" to noteId,
    "userId" to userId,
    "title" to title,
    "score" to score,
    "completed" to completed,
    "createdAt" to createdAt.time,
    "questions" to questions.map { mapOf(
        "id" to it.id,
        "question" to it.question,
        "options" to it.options,
        "correctAnswerIndex" to it.correctAnswerIndex,
        "explanation" to it.explanation
    )}
)

fun com.google.firebase.firestore.DocumentSnapshot.toQuiz(): Quiz? = try {
    val questionsData = get("questions") as? List<Map<String, Any>> ?: emptyList()
    Quiz(
        id = getString("id") ?: id,
        noteId = getString("noteId") ?: "",
        userId = getString("userId") ?: "",
        title = getString("title") ?: "",
        score = getLong("score")?.toInt() ?: 0,
        completed = getBoolean("completed") ?: false,
        createdAt = Date(getLong("createdAt") ?: System.currentTimeMillis()),
        questions = questionsData.map {
            QuizQuestion(
                id = it["id"] as? String ?: "",
                question = it["question"] as? String ?: "",
                options = it["options"] as? List<String> ?: emptyList(),
                correctAnswerIndex = (it["correctAnswerIndex"] as? Long)?.toInt() ?: 0,
                explanation = it["explanation"] as? String ?: ""
            )
        }
    )
} catch (e: Exception) { null }

fun StudyPlan.toFirebaseMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "userId" to userId,
    "title" to title,
    "tasks" to tasks.map { task -> mapOf(
        "id" to task.id,
        "title" to task.title,
        "description" to task.description,
        "dueDate" to task.dueDate.time,
        "isCompleted" to task.isCompleted,
        "priority" to task.priority.name
    )},
    "startDate" to startDate.time,
    "endDate" to endDate.time,
    "createdAt" to createdAt.time,
    "source" to source.name
)

fun com.google.firebase.firestore.DocumentSnapshot.toStudyPlan(): StudyPlan? = try {
    val tasksData = get("tasks") as? List<Map<String, Any>> ?: emptyList()
    StudyPlan(
        id = getString("id") ?: id,
        userId = getString("userId") ?: "",
        title = getString("title") ?: "",
        tasks = tasksData.map { m ->
            StudyTask(
                id = m["id"] as? String ?: "",
                title = m["title"] as? String ?: "",
                description = m["description"] as? String ?: "",
                dueDate = Date((m["dueDate"] as? Long) ?: System.currentTimeMillis()),
                isCompleted = m["isCompleted"] as? Boolean ?: false,
                priority = try { Priority.valueOf(m["priority"] as? String ?: "MEDIUM") } catch (e: Exception) { Priority.MEDIUM }
            )
        },
        startDate = Date(getLong("startDate") ?: System.currentTimeMillis()),
        endDate = Date(getLong("endDate") ?: System.currentTimeMillis()),
        createdAt = Date(getLong("createdAt") ?: System.currentTimeMillis()),
        source = try { PlanSource.valueOf(getString("source") ?: "AI") } catch (e: Exception) { PlanSource.AI }
    )
} catch (e: Exception) { null }

fun Subject.toFirebaseMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "userId" to userId,
    "name" to name,
    "colorHex" to colorHex,
    "emoji" to emoji,
    "createdAt" to createdAt.time
)

fun com.google.firebase.firestore.DocumentSnapshot.toSubject(): Subject? = try {
    Subject(
        id = getString("id") ?: id,
        userId = getString("userId") ?: "",
        name = getString("name") ?: "",
        colorHex = getString("colorHex") ?: "#87CEFA",
        emoji = getString("emoji") ?: "📚",
        createdAt = Date(getLong("createdAt") ?: System.currentTimeMillis())
    )
} catch (e: Exception) { null }

fun GradeEntry.toFirebaseMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "userId" to userId,
    "quizId" to quizId,
    "noteId" to noteId,
    "noteTitle" to noteTitle,
    "subjectId" to subjectId,
    "score" to score,
    "total" to total,
    "percentage" to percentage,
    "grade" to grade,
    "createdAt" to createdAt.time
)

fun com.google.firebase.firestore.DocumentSnapshot.toGradeEntry(): GradeEntry? = try {
    GradeEntry(
        id = getString("id") ?: id,
        userId = getString("userId") ?: "",
        quizId = getString("quizId") ?: "",
        noteId = getString("noteId") ?: "",
        noteTitle = getString("noteTitle") ?: "",
        subjectId = getString("subjectId") ?: "",
        score = (getLong("score") ?: getDouble("score")?.toLong() ?: 0L).toInt(),
        total = (getLong("total") ?: getDouble("total")?.toLong() ?: 0L).toInt(),
        percentage = (getLong("percentage") ?: getDouble("percentage")?.toLong() ?: 0L).toInt(),
        grade = getString("grade") ?: "",
        createdAt = Date(getLong("createdAt") ?: System.currentTimeMillis())
    )
} catch (e: Exception) { null }

fun Flashcard.toFirebaseMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "noteId" to noteId,
    "userId" to userId,
    "front" to front,
    "back" to back,
    "createdAt" to createdAt.time
)

fun ChatMessage.toFirebaseMap(): Map<String, Any?> = mapOf(
    "id"        to id,
    "role"      to role.name,
    "content"   to content,
    "timestamp" to timestamp.time
)

fun com.google.firebase.firestore.DocumentSnapshot.toChatMessage(): ChatMessage? {
    val id = getString("id") ?: return null
    val role = getString("role") ?: return null
    val content = getString("content") ?: return null
    val timestamp = getLong("timestamp") ?: return null
    return ChatMessage(
        id = id,
        role = ChatRole.valueOf(role),
        content = content,
        timestamp = Date(timestamp)
    )
}

fun StudyGroup.toFirebaseMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "name" to name,
    "description" to description,
    "createdBy" to createdBy,
    "createdAt" to createdAt.time,
    "inviteLink" to inviteLink,
    "isActive" to isActive,
    "isPrivate" to isPrivate,
    "password" to password,
    "topic" to topic
)

fun com.google.firebase.firestore.DocumentSnapshot.toStudyGroup(): StudyGroup? = try {
    StudyGroup(
        id = getString("id") ?: id,
        name = getString("name") ?: "",
        description = getString("description") ?: "",
        createdBy = getString("createdBy") ?: "",
        createdAt = Date(getLong("createdAt") ?: System.currentTimeMillis()),
        inviteLink = getString("inviteLink") ?: "",
        isActive = getBoolean("isActive") ?: true,
        isPrivate = getBoolean("isPrivate") ?: false,
        password = getString("password") ?: "",
        topic = getString("topic") ?: ""
    )
} catch (e: Exception) { null }

fun StudyGroupMember.toFirebaseMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "groupId" to groupId,
    "userId" to userId,
    "userName" to userName,
    "userEmail" to userEmail,
    "joinedAt" to joinedAt.time,
    "role" to role.name
)

fun com.google.firebase.firestore.DocumentSnapshot.toStudyGroupMember(): StudyGroupMember? = try {
    StudyGroupMember(
        id = getString("id") ?: id,
        groupId = getString("groupId") ?: "",
        userId = getString("userId") ?: "",
        userName = getString("userName") ?: "",
        userEmail = getString("userEmail") ?: "",
        joinedAt = Date(getLong("joinedAt") ?: System.currentTimeMillis()),
        role = GroupMemberRole.valueOf(getString("role") ?: "MEMBER")
    )
} catch (e: Exception) { null }

fun GroupMessage.toFirebaseMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "groupId" to groupId,
    "senderId" to senderId,
    "senderName" to senderName,
    "message" to message,
    "timestamp" to timestamp.time,
    "isEdited" to isEdited
)

fun com.google.firebase.firestore.DocumentSnapshot.toGroupMessage(): GroupMessage? = try {
    GroupMessage(
        id = getString("id") ?: id,
        groupId = getString("groupId") ?: "",
        senderId = getString("senderId") ?: "",
        senderName = getString("senderName") ?: "",
        message = getString("message") ?: "",
        timestamp = Date(getLong("timestamp") ?: System.currentTimeMillis()),
        isEdited = getBoolean("isEdited") ?: false
    )
} catch (e: Exception) { null }
