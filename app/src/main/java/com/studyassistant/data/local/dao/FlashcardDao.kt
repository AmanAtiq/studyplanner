package com.studyassistant.data.local.dao

import androidx.room.*
import com.studyassistant.data.local.entity.FlashcardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FlashcardDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcards(cards: List<FlashcardEntity>)

    @Query("SELECT * FROM flashcards WHERE noteId = :noteId")
    fun getFlashcardsForNote(noteId: String): Flow<List<FlashcardEntity>>

    @Query("DELETE FROM flashcards WHERE noteId = :noteId")
    suspend fun deleteFlashcardsForNote(noteId: String)

    @Query("DELETE FROM flashcards")
    suspend fun deleteAllFlashcards()
}
