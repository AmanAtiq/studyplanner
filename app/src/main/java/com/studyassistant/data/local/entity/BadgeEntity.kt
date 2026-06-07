package com.studyassistant.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "badges")
data class BadgeEntity(
    @PrimaryKey val id: String,
    val name: String = "",
    val description: String = "",
    val emoji: String = "",
    val earnedAt: Long? = null
)

fun BadgeEntity.toDomain(): com.studyassistant.domain.model.Badge = com.studyassistant.domain.model.Badge(
    id = id,
    name = name,
    description = description,
    emoji = emoji,
    earnedAt = earnedAt?.let { Date(it) }
)

fun com.studyassistant.domain.model.Badge.toEntity(): BadgeEntity = BadgeEntity(
    id = id,
    name = name,
    description = description,
    emoji = emoji,
    earnedAt = earnedAt?.time
)

