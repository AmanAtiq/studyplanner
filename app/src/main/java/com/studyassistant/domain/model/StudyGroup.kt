package com.studyassistant.domain.model

import java.util.Date

data class StudyGroup(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val createdBy: String = "",
    val createdAt: Date = Date(),
    val inviteLink: String = "",
    val isActive: Boolean = true,
    val isPrivate: Boolean = false,
    val password: String = "",
    val memberCount: Int = 0,
    val topic: String = ""
)

data class StudyGroupMember(
    val id: String = "",
    val groupId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val joinedAt: Date = Date(),
    val role: GroupMemberRole = GroupMemberRole.MEMBER
)

data class GroupMessage(
    val id: String = "",
    val groupId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val message: String = "",
    val timestamp: Date = Date(),
    val isEdited: Boolean = false
)

enum class GroupMemberRole {
    ADMIN,
    MEMBER
}
