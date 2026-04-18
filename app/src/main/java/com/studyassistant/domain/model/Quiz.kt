package com.studyassistant.domain.model

import java.util.Date

// ── Quiz ──────────────────────────────────────────────
data class Quiz(
    val id: String = "",
    val noteId: String = "",
    val userId: String = "",
    val title: String = "",
    val questions: List<QuizQuestion> = emptyList(),
    val score: Int = 0,
    val completed: Boolean = false,
    val createdAt: Date = Date()
)
