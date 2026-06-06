
package com.studyassistant.domain.model

import java.util.Date

// ── Note ──────────────────────────────────────────────
data class Note(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val originalContent: String = "",
    val summary: String = "",
    val language: AppLanguage = AppLanguage.ENGLISH,
    val fileUrl: String = "",
    val fileType: FileType = FileType.TEXT,
    val subjectId: String = "",
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)

enum class FileType { TEXT, PDF, IMAGE }
enum class AppLanguage { ENGLISH, URDU }
