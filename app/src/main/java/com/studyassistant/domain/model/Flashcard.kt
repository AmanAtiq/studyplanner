package com.studyassistant.domain.model

import java.util.Date

data class Flashcard(
    val id: String = "",
    val noteId: String = "",
    val userId: String = "",
    val front: String = "",
    val back: String = "",
    val createdAt: Date = Date()
)
