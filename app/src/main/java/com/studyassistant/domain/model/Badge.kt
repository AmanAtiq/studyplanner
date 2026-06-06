package com.studyassistant.domain.model

import java.util.Date

data class Badge(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val emoji: String = "",
    val earnedAt: Date? = null
) {
    val isEarned: Boolean get() = earnedAt != null
}

object BadgeDefinitions {
    val ALL = listOf(
        Badge("first_quiz",      "First Step",        "Complete your first quiz",         "🎯"),
        Badge("perfect_score",   "Perfect!",          "Score 100% on a quiz",             "💯"),
        Badge("five_quizzes",    "Quiz Enthusiast",   "Complete 5 quizzes",               "🔥"),
        Badge("ten_quizzes",     "Quiz Master",       "Complete 10 quizzes",              "🏆"),
        Badge("first_a",         "Top Student",       "Score an A or A+ on any quiz",     "⭐"),
        Badge("three_as",        "Scholar",           "Score an A grade three times",     "🎓"),
        Badge("first_subject",   "Organized",         "Create your first subject",        "📁"),
        Badge("five_notes",      "Knowledge Seeker",  "Upload 5 notes",                   "📚")
    )

    fun byId(id: String): Badge? = ALL.firstOrNull { it.id == id }
}
