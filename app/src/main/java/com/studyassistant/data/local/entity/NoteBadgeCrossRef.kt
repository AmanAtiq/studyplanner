package com.studyassistant.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "note_badge_junction",
    primaryKeys = ["noteId", "badgeId"],
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class NoteBadgeCrossRef(
    val noteId: String,
    val badgeId: String,
    val earnedAt: Long = System.currentTimeMillis()
)
