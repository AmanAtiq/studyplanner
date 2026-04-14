package com.studyassistant.domain.model

import java.util.Date


// ── StudyPlan ─────────────────────────────────────────
data class StudyPlan(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val tasks: List<StudyTask> = emptyList(),
    val startDate: Date = Date(),
    val endDate: Date = Date(),
    val createdAt: Date = Date()
)

data class StudyTask(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val dueDate: Date = Date(),
    val isCompleted: Boolean = false,
    val priority: Priority = Priority.MEDIUM
)

enum class Priority { LOW, MEDIUM, HIGH }
