package com.studyassistant.domain.model

import java.util.Date

enum class ChatRole { USER, AI }

data class ChatMessage(
    val id: String = "",
    val role: ChatRole = ChatRole.USER,
    val content: String = "",
    val timestamp: Date = Date()
)
