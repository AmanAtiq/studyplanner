package com.studyassistant.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quizzes")
data class QuizEntity(
    @PrimaryKey val id: String,
    val noteId: String,
    val userId: String,
    val questionsJson: String,   // JSON serialized
    val score: Int,
    val completed: Boolean,
    val createdAt: Long
)
