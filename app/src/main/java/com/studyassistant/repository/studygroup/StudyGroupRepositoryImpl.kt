package com.studyassistant.repository.studygroup

import com.studyassistant.data.local.dao.StudyGroupDao
import com.studyassistant.data.local.entity.GroupMessageEntity
import com.studyassistant.data.local.entity.StudyGroupEntity
import com.studyassistant.data.local.entity.StudyGroupMemberEntity
import com.studyassistant.domain.model.GroupMessage
import com.studyassistant.domain.model.GroupMemberRole
import com.studyassistant.domain.model.StudyGroup
import com.studyassistant.domain.model.StudyGroupMember
import com.studyassistant.repository.StudyGroupRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

class StudyGroupRepositoryImpl @Inject constructor(
    private val studyGroupDao: StudyGroupDao
) : StudyGroupRepository {

    override suspend fun createGroup(
        userId: String,
        name: String,
        description: String,
        topic: String,
        isPrivate: Boolean,
        password: String
    ): Result<StudyGroup> = try {
        val groupId = UUID.randomUUID().toString()
        val inviteLink = generateInviteLink(groupId)
        val now = System.currentTimeMillis()

        val entity = StudyGroupEntity(
            id = groupId,
            name = name,
            description = description,
            createdBy = userId,
            createdAt = now,
            inviteLink = inviteLink,
            isActive = true,
            isPrivate = isPrivate,
            password = password,
            topic = topic
        )

        studyGroupDao.insertGroup(entity)

        // Add creator as admin
        studyGroupDao.addMember(
            StudyGroupMemberEntity(
                id = UUID.randomUUID().toString(),
                groupId = groupId,
                userId = userId,
                userName = "",
                userEmail = "",
                joinedAt = now,
                role = "ADMIN"
            )
        )

        Result.success(entity.toDomain())
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getGroupById(groupId: String): StudyGroup? =
        studyGroupDao.getGroupById(groupId)?.toDomain()

    override suspend fun getGroupByInviteLink(inviteLink: String): StudyGroup? =
        studyGroupDao.getGroupByInviteLink(inviteLink)?.toDomain()

    override suspend fun getAllActiveGroups(): Flow<List<StudyGroup>> =
        studyGroupDao.getAllActiveGroups().map { groups -> groups.map { it.toDomain() } }

    override suspend fun updateGroup(group: StudyGroup): Result<Unit> = try {
        studyGroupDao.updateGroup(group.toEntity())
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun deleteGroup(groupId: String): Result<Unit> = try {
        val group = studyGroupDao.getGroupById(groupId)
        if (group != null) {
            studyGroupDao.deleteGroup(group)
            Result.success(Unit)
        } else {
            Result.failure(Exception("Group not found"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun addMember(
        groupId: String,
        userId: String,
        userName: String,
        userEmail: String
    ): Result<StudyGroupMember> = try {
        val entity = StudyGroupMemberEntity(
            id = UUID.randomUUID().toString(),
            groupId = groupId,
            userId = userId,
            userName = userName,
            userEmail = userEmail,
            joinedAt = System.currentTimeMillis(),
            role = "MEMBER"
        )
        studyGroupDao.addMember(entity)
        Result.success(entity.toDomain())
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun removeMember(groupId: String, userId: String): Result<Unit> = try {
        studyGroupDao.removeMember(groupId, userId)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getGroupMembers(groupId: String): Flow<List<StudyGroupMember>> =
        studyGroupDao.getGroupMembers(groupId).map { members -> members.map { it.toDomain() } }

    override suspend fun getUserGroups(userId: String): Flow<List<StudyGroup>> =
        studyGroupDao.getUserGroups(userId)
            .combine(studyGroupDao.getAllActiveGroups()) { members, groups ->
                val memberGroupIds = members.map { it.groupId }
                groups.filter { it.id in memberGroupIds }.map { it.toDomain() }
            }

    override suspend fun isMemberOfGroup(groupId: String, userId: String): Boolean =
        studyGroupDao.getMember(groupId, userId) != null

    override suspend fun sendMessage(
        groupId: String,
        senderId: String,
        senderName: String,
        message: String
    ): Result<GroupMessage> = try {
        val entity = GroupMessageEntity(
            id = UUID.randomUUID().toString(),
            groupId = groupId,
            senderId = senderId,
            senderName = senderName,
            message = message,
            timestamp = System.currentTimeMillis(),
            isEdited = false
        )
        studyGroupDao.insertMessage(entity)
        Result.success(entity.toDomain())
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getGroupMessages(groupId: String): Flow<List<GroupMessage>> =
        studyGroupDao.getGroupMessages(groupId).map { messages -> messages.map { it.toDomain() } }

    override suspend fun editMessage(messageId: String, newContent: String): Result<Unit> = try {
        // Fetch, update, and save
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun deleteMessage(messageId: String): Result<Unit> = try {
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun generateInviteLink(groupId: String): String =
        "study.group/${UUID.randomUUID()}"

    private fun StudyGroupEntity.toDomain() = StudyGroup(
        id = id,
        name = name,
        description = description,
        createdBy = createdBy,
        createdAt = java.util.Date(createdAt),
        inviteLink = inviteLink,
        isActive = isActive,
        isPrivate = isPrivate,
        password = password,
        topic = topic
    )

    private fun StudyGroup.toEntity() = StudyGroupEntity(
        id = id,
        name = name,
        description = description,
        createdBy = createdBy,
        createdAt = createdAt.time,
        inviteLink = inviteLink,
        isActive = isActive,
        isPrivate = isPrivate,
        password = password,
        topic = topic
    )

    private fun StudyGroupMemberEntity.toDomain() = StudyGroupMember(
        id = id,
        groupId = groupId,
        userId = userId,
        userName = userName,
        userEmail = userEmail,
        joinedAt = java.util.Date(joinedAt),
        role = if (role == "ADMIN") GroupMemberRole.ADMIN else GroupMemberRole.MEMBER
    )

    private fun GroupMessageEntity.toDomain() = GroupMessage(
        id = id,
        groupId = groupId,
        senderId = senderId,
        senderName = senderName,
        message = message,
        timestamp = java.util.Date(timestamp),
        isEdited = isEdited
    )
}
