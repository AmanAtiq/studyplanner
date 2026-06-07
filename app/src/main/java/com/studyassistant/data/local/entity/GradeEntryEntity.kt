package com.studyassistant.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.studyassistant.domain.model.GradeEntry
import java.util.Date

@Entity(tableName = "grade_entries")
data class GradeEntryEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val quizId: String,
    val noteId: String,
    val noteTitle: String,
    val subjectId: String,
    val score: Int,
    val total: Int,
    val percentage: Int,
    val grade: String,
    val createdAt: Long
)

fun GradeEntryEntity.toDomain() = GradeEntry(
    id = id,
    userId = userId,
    quizId = quizId,
    noteId = noteId,
    noteTitle = noteTitle,
    subjectId = subjectId,
    score = score,
    total = total,
    percentage = percentage,
    grade = grade,
    createdAt = Date(createdAt)
)

fun GradeEntry.toEntity() = GradeEntryEntity(
    id = id,
    userId = userId,
    quizId = quizId,
    noteId = noteId,
    noteTitle = noteTitle,
    subjectId = subjectId,
    score = score,
    total = total,
    percentage = percentage,
    grade = grade,
    createdAt = createdAt.time
)
