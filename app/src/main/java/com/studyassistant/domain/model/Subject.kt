package com.studyassistant.domain.model

import java.util.Date

data class Subject(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val colorHex: String = "#87CEFA",
    val emoji: String = "📚",
    val createdAt: Date = Date()
)

val SUBJECT_COLORS = listOf(
    "#87CEFA", "#B7A1E2", "#FF8BD2", "#90EE90",
    "#FFD700", "#FFA07A", "#98D8D8", "#DDA0DD"
)

val SUBJECT_EMOJIS = listOf("📚", "🔬", "🧮", "📐", "🌍", "💻", "🎨", "🏛️", "⚗️", "📖")
