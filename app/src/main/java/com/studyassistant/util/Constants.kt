package com.studyassistant.util

import com.studyassistant.BuildConfig

object Constants {
    // DEEPSEEK_API_KEY is fetched reflectively from BuildConfig to avoid compile-time
    // reference errors when switching BuildConfig fields.
    val DEEPSEEK_API_KEY: String = run {
        // Try DEEPSEEK_API_KEY first, then fall back to ANTHROPIC_API_KEY if present.
        try {
            val f = BuildConfig::class.java.getField("DEEPSEEK_API_KEY")
            val v = f.get(null) as? String
            if (!v.isNullOrBlank()) v else try {
                val f2 = BuildConfig::class.java.getField("ANTHROPIC_API_KEY")
                f2.get(null) as? String ?: ""
            } catch (_: Exception) {
                ""
            }
        } catch (_: Exception) {
            try {
                val f2 = BuildConfig::class.java.getField("ANTHROPIC_API_KEY")
                f2.get(null) as? String ?: ""
            } catch (_: Exception) {
                ""
            }
        }
    }

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