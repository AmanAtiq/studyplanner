package com.studyassistant.domain.model

import java.util.Date

data class GradeEntry(
    val id: String = "",
    val userId: String = "",
    val quizId: String = "",
    val noteId: String = "",
    val noteTitle: String = "",
    val subjectId: String = "",
    val score: Int = 0,
    val total: Int = 0,
    val percentage: Int = 0,
    val grade: String = "",
    val createdAt: Date = Date()
)

fun gradeFromPercentage(pct: Int): String = when {
    pct >= 90 -> "A+"
    pct >= 80 -> "A"
    pct >= 70 -> "B"
    pct >= 60 -> "C"
    pct >= 50 -> "D"
    else -> "F"
}

fun gradeColor(grade: String): Long = when (grade) {
    "A+", "A" -> 0xFF4CAF50
    "B"       -> 0xFF8BC34A
    "C"       -> 0xFFFFC107
    "D"       -> 0xFFFF9800
    else      -> 0xFFF44336
}
