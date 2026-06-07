package com.studyassistant.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.studyassistant.domain.model.Flashcard
import java.util.Date

@Entity(tableName = "flashcards")
data class FlashcardEntity(
    @PrimaryKey val id: String,
    val noteId: String,
    val userId: String,
    val front: String,
    val back: String,
    val createdAt: Long
)

fun FlashcardEntity.toDomain() = Flashcard(
    id = id,
    noteId = noteId,
    userId = userId,
    front = front,
    back = back,
    createdAt = Date(createdAt)
)

fun Flashcard.toEntity() = FlashcardEntity(
    id = id,
    noteId = noteId,
    userId = userId,
    front = front,
    back = back,
    createdAt = createdAt.time
)
