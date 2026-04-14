package com.studyassistant.util

import com.studyassistant.BuildConfig

object Constants {
    // Store your API key in local.properties as ANTHROPIC_API_KEY=sk-ant-...
    // and expose via BuildConfig
    val ANTHROPIC_API_KEY: String = BuildConfig.ANTHROPIC_API_KEY

    const val MAX_QUIZ_QUESTIONS = 15
    const val DEFAULT_QUIZ_QUESTIONS = 10
    const val MAX_NOTE_CHARS = 8000

    val SUPPORTED_EXAMS = listOf("MDCAT", "ECAT", "O-Levels", "A-Levels", "Matric", "Other")
}

// Extension to truncate content to API limit
fun String.toApiSafeLength(limit: Int = Constants.MAX_NOTE_CHARS): String =
    if (this.length > limit) this.substring(0, limit) + "..." else this

// Score to percentage
fun Int.toPercent(total: Int): Int =
    if (total == 0) 0 else ((this.toFloat() / total) * 100).toInt()