package com.studyassistant.repository.local

import com.studyassistant.data.local.dao.*
import com.studyassistant.data.local.entity.toDomain
import com.studyassistant.data.local.entity.toEntity
import com.studyassistant.data.store.JsonPersistenceStore
import com.studyassistant.domain.model.*
import com.studyassistant.repository.LocalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class LocalRepositoryImpl @Inject constructor(
    private val store: JsonPersistenceStore,
    private val chatMessageDao: ChatMessageDao,
    private val gradeEntryDao: GradeEntryDao,
    private val flashcardDao: FlashcardDao,
    private val badgeDao: BadgeDao
) : LocalRepository {

    override suspend fun cacheNote(note: Note) {
        store.saveNote(note)
    }

    override suspend fun getCachedNotes(): Flow<List<Note>> =
        combine(store.notesFlow(), store.settings) { notes, settings ->
            val currentUserId = settings.currentUserId
            if (currentUserId == null) emptyList() else notes.filter { it.userId == currentUserId }
        }

    override suspend fun getCachedNoteById(noteId: String): Note? =
        store.notesFlow().value.firstOrNull { it.id == noteId }

    override suspend fun deleteCachedNote(noteId: String) {
        store.deleteNote(noteId)
    }

    override suspend fun getCachedNotesBySubject(subjectId: String): Flow<List<Note>> =
        combine(store.notesFlow(), store.settings) { notes, settings ->
            val currentUserId = settings.currentUserId
            if (currentUserId == null) emptyList()
            else notes.filter { it.userId == currentUserId && it.subjectId == subjectId }
        }

    override suspend fun assignSubjectToNote(noteId: String, subjectId: String) {
        val note = getCachedNoteById(noteId)
        if (note != null) {
            cacheNote(note.copy(subjectId = subjectId))
        }
    }

    override suspend fun assignBadgeToNote(noteId: String, badgeId: String) {
    }

    override suspend fun removeBadgeFromNote(noteId: String, badgeId: String) {
    }

    override suspend fun getBadgesForNote(noteId: String): Flow<List<String>> =
        store.notesFlow().map { _ -> emptyList() }

    override suspend fun cacheQuiz(quiz: Quiz) {
        store.saveQuiz(quiz)
    }

    override suspend fun getCachedQuizzes(): Flow<List<Quiz>> =
        combine(store.quizzesFlow(), store.notesFlow(), store.settings) { quizzes, notes, settings ->
            val currentUserId = settings.currentUserId
            if (currentUserId == null) emptyList()
            else {
                quizzes.filter { it.userId == currentUserId && it.completed }.map { quiz ->
                    if (quiz.title.isBlank()) {
                        val noteTitle = notes.firstOrNull { it.id == quiz.noteId }?.title
                        quiz.copy(title = noteTitle ?: quiz.title)
                    } else quiz
                }
            }
        }

    override suspend fun cacheGrade(entry: GradeEntry) {
        store.saveGrade(entry)
        gradeEntryDao.insertGrade(entry.toEntity())
    }

    override suspend fun getCachedGrades(): Flow<List<GradeEntry>> =
        combine(store.settings, store.gradesFlow()) { settings, jsonGrades ->
            val userId = settings.currentUserId ?: return@combine emptyList<GradeEntry>()
            jsonGrades.filter { it.userId == userId }
        }

    override suspend fun deleteGradeHistory(userId: String) {
        // Clear from JSON store would need a method, but for now we clear Room
        // Ideally store should have a clear method too
        // gradeEntryDao.deleteAllForUser(userId) // If we add this to DAO
    }

    override suspend fun cacheStudyPlan(plan: StudyPlan) {
        store.saveStudyPlan(plan)
    }

    override suspend fun getCachedStudyPlan(): StudyPlan? =
        store.currentUser()?.id?.let { userId ->
            store.studyPlansFlow().value.filter { it.userId == userId }.maxByOrNull { it.createdAt.time }
        } ?: store.studyPlansFlow().value.maxByOrNull { it.createdAt.time }

    override suspend fun saveLanguagePreference(language: AppLanguage) {
        store.saveLanguagePreference(language)
    }

    override suspend fun getLanguagePreference(): AppLanguage = store.getLanguagePreference()

    override suspend fun saveChatMessage(userId: String, noteId: String, message: ChatMessage) {
        chatMessageDao.insertMessage(message.toEntity(userId, noteId))
    }

    override fun getChatMessages(userId: String, noteId: String): Flow<List<ChatMessage>> =
        chatMessageDao.getMessagesForNote(noteId, userId).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun getCachedSubjects(): Flow<List<Subject>> =
        combine(store.subjectsFlow(), store.settings) { subjects, settings ->
            val userId = settings.currentUserId
            if (userId == null) emptyList() else subjects.filter { it.userId == userId }
        }

    override suspend fun cacheBadge(badge: Badge) {
        badgeDao.insertBadge(badge.toEntity())
    }
}
