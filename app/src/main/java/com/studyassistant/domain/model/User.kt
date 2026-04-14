package com.studyassistant.domain.model

import java.util.Date

// ── User ──────────────────────────────────────────────
data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val preferredLanguage: AppLanguage = AppLanguage.ENGLISH,
    val targetExam: String = "",   // e.g. MDCAT, ECAT, O-Levels
    val createdAt: Date = Date()
)