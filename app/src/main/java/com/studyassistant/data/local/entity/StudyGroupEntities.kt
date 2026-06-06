package com.studyassistant.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey

@Entity(tableName = "study_groups")
data class StudyGroupEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val createdBy: String,
    val createdAt: Long,
    val inviteLink: String,
    val isActive: Boolean,
    @ColumnInfo(defaultValue = "0") val isPrivate: Boolean,
    @ColumnInfo(defaultValue = "") val password: String,
    val topic: String
)

@Entity(
    tableName = "study_group_members",
    primaryKeys = ["id"],
    foreignKeys = [
        ForeignKey(
            entity = StudyGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class StudyGroupMemberEntity(
    val id: String,
    val groupId: String,
    val userId: String,
    val userName: String,
    val userEmail: String,
    val joinedAt: Long,
    val role: String
)

@Entity(
    tableName = "group_messages",
    primaryKeys = ["id"],
    foreignKeys = [
        ForeignKey(
            entity = StudyGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class GroupMessageEntity(
    val id: String,
    val groupId: String,
    val senderId: String,
    val senderName: String,
    val message: String,
    val timestamp: Long,
    val isEdited: Boolean
)
