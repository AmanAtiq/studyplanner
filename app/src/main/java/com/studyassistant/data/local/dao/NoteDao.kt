package com.studyassistant.data.local.dao

import androidx.room.*
import com.studyassistant.data.local.entity.NoteEntity
import com.studyassistant.data.local.entity.NoteBadgeCrossRef
import com.studyassistant.data.local.entity.QuizEntity
import com.studyassistant.data.local.entity.StudyPlanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY createdAt DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: String): NoteEntity?

    @Query("SELECT * FROM notes WHERE subjectId = :subjectId ORDER BY createdAt DESC")
    fun getNotesBySubject(subjectId: String): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity)

    @Delete
    suspend fun deleteNote(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteById(id: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun assignBadgeToNote(crossRef: NoteBadgeCrossRef)

    @Query("SELECT badgeId FROM note_badge_junction WHERE noteId = :noteId ORDER BY earnedAt DESC")
    fun getBadgesForNote(noteId: String): Flow<List<String>>

    @Query("DELETE FROM note_badge_junction WHERE noteId = :noteId AND badgeId = :badgeId")
    suspend fun removeBadgeFromNote(noteId: String, badgeId: String)
}
