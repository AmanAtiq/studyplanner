package com.studyassistant.repository.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.studyassistant.data.local.dao.NoteDao
import com.studyassistant.data.local.dao.QuizDao
import com.studyassistant.data.local.dao.StudyPlanDao
import com.studyassistant.data.local.entity.NoteEntity
import com.studyassistant.data.local.entity.QuizEntity
import com.studyassistant.data.local.entity.StudyPlanEntity
import com.studyassistant.domain.model.*
import com.studyassistant.repository.LocalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class LocalRepositoryImpl @Inject constructor(
    private val noteDao: NoteDao,
    private val quizDao: QuizDao,
    private val studyPlanDao: StudyPlanDao,
    private val dataStore: DataStore<Preferences>
) : LocalRepository {

    private val gson = Gson()
    private val LANG_KEY = stringPreferencesKey("language")

    // ── Notes ─────────────────────────────────────────
    override suspend fun cacheNote(note: Note) {
        noteDao.insertNote(note.toEntity())
    }

    override suspend fun getCachedNotes(): Flow<List<Note>> =
        noteDao.getAllNotes().map { list -> list.map { it.toDomain() } }

    override suspend fun getCachedNoteById(noteId: String): Note? =
        noteDao.getNoteById(noteId)?.toDomain()

    override suspend fun deleteCachedNote(noteId: String) {
        noteDao.deleteById(noteId)
    }

    // ── Quizzes ───────────────────────────────────────
    override suspend fun cacheQuiz(quiz: Quiz) {
        quizDao.insertQuiz(quiz.toEntity())
    }

    override suspend fun getCachedQuizzes(): Flow<List<Quiz>> =
        quizDao.getAllQuizzes().map { list -> list.map { it.toDomain() } }

    // ── Study Plan ────────────────────────────────────
    override suspend fun cacheStudyPlan(plan: StudyPlan) {
        studyPlanDao.clearStudyPlans()
        studyPlanDao.insertStudyPlan(plan.toEntity())
    }

    override suspend fun getCachedStudyPlan(): StudyPlan? =
        studyPlanDao.getStudyPlan()?.toDomain()

    // ── Preferences ───────────────────────────────────
    override suspend fun saveLanguagePreference(language: AppLanguage) {
        dataStore.edit { prefs -> prefs[LANG_KEY] = language.name }
    }

    override suspend fun getLanguagePreference(): AppLanguage {
        val prefs = dataStore.data.first()
        return AppLanguage.valueOf(prefs[LANG_KEY] ?: AppLanguage.ENGLISH.name)
    }

    // ── Mappers ───────────────────────────────────────
    private fun Note.toEntity() = NoteEntity(
        id = id, userId = userId, title = title,
        originalContent = originalContent, summary = summary,
        language = language.name, fileUrl = fileUrl,
        fileType = fileType.name,
        createdAt = createdAt.time, updatedAt = updatedAt.time
    )

    private fun NoteEntity.toDomain() = Note(
        id = id, userId = userId, title = title,
        originalContent = originalContent, summary = summary,
        language = AppLanguage.valueOf(language),
        fileUrl = fileUrl, fileType = FileType.valueOf(fileType),
        createdAt = java.util.Date(createdAt),
        updatedAt = java.util.Date(updatedAt)
    )

    private fun Quiz.toEntity() = QuizEntity(
        id = id, noteId = noteId, userId = userId,
        questionsJson = gson.toJson(questions),
        score = score, completed = completed,
        createdAt = createdAt.time
    )

    private fun QuizEntity.toDomain(): Quiz {
        val type = object : TypeToken<List<QuizQuestion>>() {}.type
        return Quiz(
            id = id, noteId = noteId, userId = userId,
            questions = gson.fromJson(questionsJson, type),
            score = score, completed = completed,
            createdAt = java.util.Date(createdAt)
        )
    }

    private fun StudyPlan.toEntity() = StudyPlanEntity(
        id = id, userId = userId, title = title,
        tasksJson = gson.toJson(tasks),
        startDate = startDate.time, endDate = endDate.time,
        createdAt = createdAt.time
    )

    private fun StudyPlanEntity.toDomain(): StudyPlan {
        val type = object : TypeToken<List<StudyTask>>() {}.type
        return StudyPlan(
            id = id, userId = userId, title = title,
            tasks = gson.fromJson(tasksJson, type),
            startDate = java.util.Date(startDate),
            endDate = java.util.Date(endDate),
            createdAt = java.util.Date(createdAt)
        )
    }
}