package com.studyassistant.util

import com.studyassistant.BuildConfig

object Constants {
    // GEMINI_API_KEY is fetched reflectively to stay resilient across local setups.
    val GEMINI_API_KEY: String = run {
        // Try GEMINI_API_KEY first, then fall back to legacy keys if present.
        try {
            val f = BuildConfig::class.java.getField("GEMINI_API_KEY")
            val v = f.get(null) as? String
            if (!v.isNullOrBlank()) v else try {
                val f2 = BuildConfig::class.java.getField("DEEPSEEK_API_KEY")
                val v2 = f2.get(null) as? String
                if (!v2.isNullOrBlank()) v2 else try {
                    val f3 = BuildConfig::class.java.getField("ANTHROPIC_API_KEY")
                    f3.get(null) as? String ?: ""
                } catch (_: Exception) {
                    ""
                }
            } catch (_: Exception) {
                ""
            }
        } catch (_: Exception) {
            try {
                val f2 = BuildConfig::class.java.getField("DEEPSEEK_API_KEY")
                val v2 = f2.get(null) as? String
                if (!v2.isNullOrBlank()) v2 else try {
                    val f3 = BuildConfig::class.java.getField("ANTHROPIC_API_KEY")
                    f3.get(null) as? String ?: ""
                } catch (_: Exception) {
                    ""
                }
            } catch (_: Exception) {
                ""
            }
        }
    }

    const val GEMINI_MODEL = "gemini-2.5-flash-lite"

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