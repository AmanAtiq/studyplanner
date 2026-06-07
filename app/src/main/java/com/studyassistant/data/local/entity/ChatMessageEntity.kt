package com.studyassistant.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.studyassistant.domain.model.ChatMessage
import com.studyassistant.domain.model.ChatRole
import java.util.Date

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val noteId: String,
    val role: String,
    val content: String,
    val timestamp: Long
)

fun ChatMessageEntity.toDomain() = ChatMessage(
    id = id,
    role = ChatRole.valueOf(role),
    content = content,
    timestamp = Date(timestamp)
)

fun ChatMessage.toEntity(userId: String, noteId: String) = ChatMessageEntity(
    id = id,
    userId = userId,
    noteId = noteId,
    role = role.name,
    content = content,
    timestamp = timestamp.time
)
