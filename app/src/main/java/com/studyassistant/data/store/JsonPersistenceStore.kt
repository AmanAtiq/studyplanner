package com.studyassistant.data.store
import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.studyassistant.domain.model.AppLanguage
import com.studyassistant.domain.model.Badge
import com.studyassistant.domain.model.BadgeDefinitions
import com.studyassistant.domain.model.Flashcard
import com.studyassistant.domain.model.GradeEntry
import com.studyassistant.domain.model.Note
import com.studyassistant.domain.model.Quiz
import com.studyassistant.domain.model.StreakData
import com.studyassistant.domain.model.StudyPlan
import com.studyassistant.domain.model.Subject
import com.studyassistant.domain.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.ktx.auth
import kotlinx.coroutines.tasks.await
data class StoredAuthUser(
    val user: User,
    val password: String
)

data class AppSettings(
    val currentUserId: String? = null,
    val language: AppLanguage = AppLanguage.ENGLISH
)

data class StoredBadge(
    val userId: String = "",
    val badgeId: String = "",
    val earnedAt: Date = Date()
)

@Singleton
class JsonPersistenceStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val gson: Gson = GsonBuilder()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        .create()

    private val mutex = Mutex()
    private val authFile = File(context.filesDir, "auth_users.json")
    private val notesFile = File(context.filesDir, "notes.json")
    private val quizzesFile = File(context.filesDir, "quizzes.json")
    private val studyPlansFile = File(context.filesDir, "study_plans.json")
    private val settingsFile = File(context.filesDir, "settings.json")
    private val attachmentsDir = File(context.filesDir, "attachments")
    private val subjectsFile = File(context.filesDir, "subjects.json")
    private val gradesFile = File(context.filesDir, "grades.json")
    private val badgesFile = File(context.filesDir, "badges.json")
    private val streaksFile = File(context.filesDir, "streaks.json")
    private val flashcardsFile = File(context.filesDir, "flashcards.json")

    private val authUsersFlow = MutableStateFlow(loadAuthUsers())
    private val notesFlow = MutableStateFlow(loadNotes())
    private val quizzesFlow = MutableStateFlow(loadQuizzes())
    private val studyPlansFlow = MutableStateFlow(loadStudyPlans())
    private val settingsFlow = MutableStateFlow(loadSettings())
    private val subjectsStateFlow = MutableStateFlow(loadSubjects())
    private val gradesStateFlow = MutableStateFlow(loadGrades())
    private val badgesStateFlow = MutableStateFlow(loadStoredBadges())
    private val streaksStateFlow = MutableStateFlow(loadStreaks())
    private val flashcardsStateFlow = MutableStateFlow(loadFlashcards())

    init {
        attachmentsDir.mkdirs()
    }

    val authUsers: StateFlow<List<StoredAuthUser>> = authUsersFlow
    val notes: StateFlow<List<Note>> = notesFlow
    val quizzes: StateFlow<List<Quiz>> = quizzesFlow
    val studyPlans: StateFlow<List<StudyPlan>> = studyPlansFlow
    val settings: StateFlow<AppSettings> = settingsFlow

    suspend fun getCurrentUser(): User? {
        return currentUser()
    }

    fun currentUser(): User? {
        val currentUserId = settingsFlow.value.currentUserId ?: return null
        return authUsersFlow.value.firstOrNull { it.user.id == currentUserId }?.user
    }

    suspend fun signUp(name: String, email: String, password: String): Result<User> = mutex.withLock {
        val users = authUsersFlow.value
        if (users.any { it.user.email.equals(email, ignoreCase = true) }) {
            return Result.failure(Exception("This email is already registered. Please sign in instead."))
        }

        val user = User(
            id = UUID.randomUUID().toString(),
            name = name,
            email = email,
            createdAt = Date()
        )
        val updatedUsers = users + StoredAuthUser(user = user, password = password)
        authUsersFlow.value = updatedUsers
        persist(authFile, updatedUsers)
        updateSettings(settingsFlow.value.copy(currentUserId = user.id))
        Result.success(user)
    }

    suspend fun signIn(email: String, password: String): Result<User> = mutex.withLock {
        val record = authUsersFlow.value.firstOrNull {
            it.user.email.equals(email, ignoreCase = true) && it.password == password
        }
        if (record == null) {
            return Result.failure(Exception("Invalid email or password."))
        }

        updateSettings(settingsFlow.value.copy(currentUserId = record.user.id))
        Result.success(record.user)
    }
    suspend fun signInWithGoogle(idToken: String): Result<User> = mutex.withLock {
        return try {
            // 1. Exchange Google token with Firebase Auth
            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
            val result = Firebase.auth.signInWithCredential(firebaseCredential).await()
            val firebaseUser = result.user
                ?: return Result.failure(Exception("Google Sign-In failed. Try again."))

            // 2. Check if this Google user already exists in local store
            val existingRecord = authUsersFlow.value
                .firstOrNull { it.user.id == firebaseUser.uid }

            val user = if (existingRecord != null) {
                // Already registered — just sign them in
                existingRecord.user
            } else {
                // New Google user — create and save them same as signUp does
                val newUser = User(
                    id = firebaseUser.uid,
                    name = firebaseUser.displayName ?: "Student",
                    email = firebaseUser.email ?: "",
                    createdAt = Date()
                )
                // Save with empty password since Google handles auth
                val updatedUsers = authUsersFlow.value + StoredAuthUser(user = newUser, password = "")
                authUsersFlow.value = updatedUsers
                persist(authFile, updatedUsers)
                newUser
            }

            // 3. Set as current logged-in user (same as signIn does)
            updateSettings(settingsFlow.value.copy(currentUserId = user.id))
            Result.success(user)

        } catch (e: Exception) {
            Result.failure(Exception("Google Sign-In failed: ${e.message}"))
        }
    }
    suspend fun signOut() = mutex.withLock {
        updateSettings(settingsFlow.value.copy(currentUserId = null))
    }

    suspend fun updateUser(user: User): Result<User> = mutex.withLock {
        val updatedUsers = authUsersFlow.value.map { record ->
            if (record.user.id == user.id) record.copy(user = user) else record
        }
        if (updatedUsers.none { it.user.id == user.id }) {
            return Result.failure(Exception("User not found"))
        }

        authUsersFlow.value = updatedUsers
        persist(authFile, updatedUsers)

        if (settingsFlow.value.currentUserId == user.id) {
            updateSettings(settingsFlow.value)
        }

        Result.success(user)
    }

    suspend fun saveNote(note: Note): Result<Note> = mutex.withLock {
        val now = Date()
        val updated = note.copy(
            id = note.id.ifBlank { UUID.randomUUID().toString() },
            createdAt = note.createdAt.takeIf { it.time > 0 } ?: now,
            updatedAt = now
        )
        val notes = notesFlow.value.toMutableList()
        val index = notes.indexOfFirst { it.id == updated.id }
        if (index >= 0) notes[index] = updated else notes.add(updated)
        notesFlow.value = notes
        persist(notesFile, notes)
        Result.success(updated)
    }

    suspend fun deleteNote(noteId: String): Result<Unit> = mutex.withLock {
        val notes = notesFlow.value.filterNot { it.id == noteId }
        notesFlow.value = notes
        persist(notesFile, notes)
        Result.success(Unit)
    }

    suspend fun saveQuiz(quiz: Quiz): Result<Quiz> = mutex.withLock {
        val updated = quiz.copy(
            id = quiz.id.ifBlank { UUID.randomUUID().toString() },
            createdAt = quiz.createdAt.takeIf { it.time > 0 } ?: Date()
        )
        val quizzes = quizzesFlow.value.toMutableList()
        val index = quizzes.indexOfFirst { it.id == updated.id }
        if (index >= 0) quizzes[index] = updated else quizzes.add(updated)
        quizzesFlow.value = quizzes
        persist(quizzesFile, quizzes)
        Result.success(updated)
    }

    suspend fun saveStudyPlan(plan: StudyPlan): Result<StudyPlan> = mutex.withLock {
        val updated = plan.copy(
            id = plan.id.ifBlank { UUID.randomUUID().toString() },
            createdAt = plan.createdAt.takeIf { it.time > 0 } ?: Date()
        )
        val plans = studyPlansFlow.value.toMutableList()
        val index = plans.indexOfFirst { it.userId == updated.userId }
        if (index >= 0) plans[index] = updated else plans.add(updated)
        studyPlansFlow.value = plans
        persist(studyPlansFile, plans)
        Result.success(updated)
    }

    suspend fun saveLanguagePreference(language: AppLanguage) = mutex.withLock {
        updateSettings(settingsFlow.value.copy(language = language))
    }

    suspend fun getLanguagePreference(): AppLanguage = settingsFlow.value.language

    suspend fun uploadAttachment(userId: String, fileBytes: ByteArray, fileName: String): Result<String> = mutex.withLock {
        return try {
            val safeName = fileName.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "file_${System.currentTimeMillis()}.bin" }
            val userDir = File(attachmentsDir, userId)
            userDir.mkdirs()
            val file = File(userDir, "${UUID.randomUUID()}_$safeName")
            file.writeBytes(fileBytes)
            Result.success(file.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun notesFlow(): StateFlow<List<Note>> = notesFlow
    fun quizzesFlow(): StateFlow<List<Quiz>> = quizzesFlow
    fun studyPlansFlow(): StateFlow<List<StudyPlan>> = studyPlansFlow
    fun subjectsFlow(): StateFlow<List<Subject>> = subjectsStateFlow
    fun gradesFlow(): StateFlow<List<GradeEntry>> = gradesStateFlow
    fun storedBadgesFlow(): StateFlow<List<StoredBadge>> = badgesStateFlow

    // ── Subject CRUD ──────────────────────────────────────
    suspend fun saveSubject(subject: Subject): Result<Subject> = mutex.withLock {
        val updated = subject.copy(id = subject.id.ifBlank { UUID.randomUUID().toString() })
        val list = subjectsStateFlow.value.toMutableList()
        val idx = list.indexOfFirst { it.id == updated.id }
        if (idx >= 0) list[idx] = updated else list.add(updated)
        subjectsStateFlow.value = list
        persist(subjectsFile, list)
        Result.success(updated)
    }

    suspend fun deleteSubject(subjectId: String): Result<Unit> = mutex.withLock {
        val list = subjectsStateFlow.value.filterNot { it.id == subjectId }
        subjectsStateFlow.value = list
        persist(subjectsFile, list)
        Result.success(Unit)
    }

    // ── Grade CRUD ────────────────────────────────────────
    suspend fun saveGrade(entry: GradeEntry): Result<GradeEntry> = mutex.withLock {
        val updated = entry.copy(id = entry.id.ifBlank { UUID.randomUUID().toString() })
        val list = gradesStateFlow.value.toMutableList()
        val idx = list.indexOfFirst { it.id == updated.id }
        if (idx >= 0) list[idx] = updated else list.add(updated)
        gradesStateFlow.value = list
        persist(gradesFile, list)
        Result.success(updated)
    }

    // ── Badge management ──────────────────────────────────
    fun getEarnedBadgeIds(userId: String): Set<String> =
        badgesStateFlow.value.filter { it.userId == userId }.map { it.badgeId }.toSet()

    suspend fun awardBadge(userId: String, badgeId: String): Result<Unit> = mutex.withLock {
        val already = badgesStateFlow.value.any { it.userId == userId && it.badgeId == badgeId }
        if (already) return Result.success(Unit)
        val list = badgesStateFlow.value + StoredBadge(userId, badgeId, Date())
        badgesStateFlow.value = list
        persist(badgesFile, list)
        Result.success(Unit)
    }

    fun getEarnedBadges(userId: String): List<Badge> {
        val earnedMap = badgesStateFlow.value
            .filter { it.userId == userId }
            .associateBy { it.badgeId }
        return BadgeDefinitions.ALL.map { def ->
            val stored = earnedMap[def.id]
            if (stored != null) def.copy(earnedAt = stored.earnedAt) else def
        }
    }

    // ── Streak management ─────────────────────────────────
    fun getStreak(userId: String): StreakData =
        streaksStateFlow.value.firstOrNull { it.userId == userId } ?: StreakData(userId = userId)

    suspend fun updateStreak(userId: String): StreakData = mutex.withLock {
        val today = java.time.LocalDate.now().toString()
        val existing = streaksStateFlow.value.firstOrNull { it.userId == userId }
            ?: StreakData(userId = userId)

        val updated = when (existing.lastActiveDateStr) {
            today -> existing  // already updated today
            java.time.LocalDate.now().minusDays(1).toString() -> {
                val newStreak = existing.currentStreak + 1
                existing.copy(currentStreak = newStreak, longestStreak = maxOf(existing.longestStreak, newStreak), lastActiveDateStr = today)
            }
            "" -> existing.copy(currentStreak = 1, longestStreak = 1, lastActiveDateStr = today)
            else -> existing.copy(currentStreak = 1, lastActiveDateStr = today)
        }

        val list = streaksStateFlow.value.toMutableList()
        val idx = list.indexOfFirst { it.userId == userId }
        if (idx >= 0) list[idx] = updated else list.add(updated)
        streaksStateFlow.value = list
        persist(streaksFile, list)
        updated
    }

    // ── Flashcard management ──────────────────────────────
    suspend fun saveFlashcards(cards: List<Flashcard>): Result<List<Flashcard>> = mutex.withLock {
        val list = flashcardsStateFlow.value.toMutableList()
        cards.forEach { card ->
            val idx = list.indexOfFirst { it.id == card.id }
            if (idx >= 0) list[idx] = card else list.add(card)
        }
        flashcardsStateFlow.value = list
        persist(flashcardsFile, list)
        Result.success(cards)
    }

    fun getFlashcardsForNote(noteId: String): List<Flashcard> =
        flashcardsStateFlow.value.filter { it.noteId == noteId }

    fun flashcardsFlow(): kotlinx.coroutines.flow.StateFlow<List<Flashcard>> = flashcardsStateFlow

    private fun loadAuthUsers(): List<StoredAuthUser> = readList(authFile, object : TypeToken<List<StoredAuthUser>>() {}.type)
    private fun loadNotes(): List<Note> = readList(notesFile, object : TypeToken<List<Note>>() {}.type)
    private fun loadQuizzes(): List<Quiz> = readList(quizzesFile, object : TypeToken<List<Quiz>>() {}.type)
    private fun loadStudyPlans(): List<StudyPlan> = readList(studyPlansFile, object : TypeToken<List<StudyPlan>>() {}.type)
    private fun loadSettings(): AppSettings = readObject(settingsFile, AppSettings::class.java) ?: AppSettings()
    private fun loadSubjects(): List<Subject> = readList(subjectsFile, object : TypeToken<List<Subject>>() {}.type)
    private fun loadGrades(): List<GradeEntry> = readList(gradesFile, object : TypeToken<List<GradeEntry>>() {}.type)
    private fun loadStoredBadges(): List<StoredBadge> = readList(badgesFile, object : TypeToken<List<StoredBadge>>() {}.type)
    private fun loadStreaks(): List<StreakData> = readList(streaksFile, object : TypeToken<List<StreakData>>() {}.type)
    private fun loadFlashcards(): List<Flashcard> = readList(flashcardsFile, object : TypeToken<List<Flashcard>>() {}.type)

    private fun updateSettings(settings: AppSettings) {
        settingsFlow.value = settings
        persist(settingsFile, settings)
    }

    private fun <T> readList(file: File, type: java.lang.reflect.Type): List<T> {
        if (!file.exists() || file.length() == 0L) return emptyList()
        return try {
            gson.fromJson(file.readText(), type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun <T> readObject(file: File, clazz: Class<T>): T? {
        if (!file.exists() || file.length() == 0L) return null
        return try {
            gson.fromJson(file.readText(), clazz)
        } catch (_: Exception) {
            null
        }
    }

    private fun persist(file: File, value: Any) {
        file.parentFile?.mkdirs()
        file.writeText(gson.toJson(value))
    }
}