package com.studyassistant.domain.model

import java.util.Date

// ── WeakArea ──────────────────────────────────────────
data class WeakArea(
    val id: String = "",
    val userId: String = "",
    val topic: String = "",
    val subject: String = "",
    val accuracy: Float = 0f,     // 0.0 - 1.0
    val totalAttempts: Int = 0,
    val suggestions: List<String> = emptyList()
)
