package com.studyassistant.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val originalContent: String,
    val summary: String,
    val language: String,
    val fileUrl: String,
    val fileType: String,
    val createdAt: Long,
    val updatedAt: Long
)
