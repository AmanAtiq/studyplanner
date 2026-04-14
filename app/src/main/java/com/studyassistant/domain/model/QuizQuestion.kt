package com.studyassistant.domain.model

import java.util.Date

data class QuizQuestion(
    val id: String = "",
    val question: String = "",
    val options: List<String> = emptyList(),
    val correctAnswerIndex: Int = 0,
    val explanation: String = "",
    val selectedAnswerIndex: Int = -1,
    val type: QuestionType = QuestionType.MCQ
)

enum class QuestionType { MCQ, TRUE_FALSE, SHORT }
