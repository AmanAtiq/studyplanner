package com.studyassistant.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "study_plans")
data class StudyPlanEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val tasksJson: String,       // JSON serialized
    val startDate: Long,
    val endDate: Long,
    val createdAt: Long
)