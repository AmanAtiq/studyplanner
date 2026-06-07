package com.studyassistant.data.local.dao

import androidx.room.*
import com.studyassistant.data.local.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE noteId = :noteId AND userId = :userId ORDER BY timestamp ASC")
    fun getMessagesForNote(noteId: String, userId: String): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages WHERE noteId = :noteId")
    suspend fun deleteMessagesForNote(noteId: String)
}
