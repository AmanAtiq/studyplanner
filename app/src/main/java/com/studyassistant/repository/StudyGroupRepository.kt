package com.studyassistant.repository

import com.studyassistant.domain.model.GroupMessage
import com.studyassistant.domain.model.StudyGroup
import com.studyassistant.domain.model.StudyGroupMember
import kotlinx.coroutines.flow.Flow

interface StudyGroupRepository {
    // Group operations
    suspend fun createGroup(userId: String, name: String, description: String, topic: String, isPrivate: Boolean, password: String): Result<StudyGroup>
    suspend fun getGroupById(groupId: String): StudyGroup?
    suspend fun getGroupByInviteLink(inviteLink: String): StudyGroup?
    suspend fun getAllActiveGroups(): Flow<List<StudyGroup>>
    suspend fun updateGroup(group: StudyGroup): Result<Unit>
    suspend fun deleteGroup(groupId: String): Result<Unit>

    // Member operations
    suspend fun addMember(groupId: String, userId: String, userName: String, userEmail: String): Result<StudyGroupMember>
    suspend fun removeMember(groupId: String, userId: String): Result<Unit>
    suspend fun getGroupMembers(groupId: String): Flow<List<StudyGroupMember>>
    suspend fun getUserGroups(userId: String): Flow<List<StudyGroup>>
    suspend fun isMemberOfGroup(groupId: String, userId: String): Boolean

    // Message operations
    suspend fun sendMessage(groupId: String, senderId: String, senderName: String, message: String): Result<GroupMessage>
    suspend fun getGroupMessages(groupId: String): Flow<List<GroupMessage>>
    suspend fun editMessage(messageId: String, newContent: String): Result<Unit>
    suspend fun deleteMessage(messageId: String): Result<Unit>

    // Invite link
    suspend fun generateInviteLink(groupId: String): String
}
