package com.studyassistant.repository.studygroup

import com.studyassistant.data.local.dao.StudyGroupDao
import com.studyassistant.data.local.entity.GroupMessageEntity
import com.studyassistant.data.local.entity.StudyGroupEntity
import com.studyassistant.data.local.entity.StudyGroupMemberEntity
import com.studyassistant.domain.model.GroupMessage
import com.studyassistant.domain.model.GroupMemberRole
import com.studyassistant.domain.model.StudyGroup
import com.studyassistant.domain.model.StudyGroupMember
import com.studyassistant.repository.FirebaseRepository
import com.studyassistant.repository.StudyGroupRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject

class StudyGroupRepositoryImpl @Inject constructor(
    private val studyGroupDao: StudyGroupDao,
    private val firebaseRepository: FirebaseRepository
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

        val group = StudyGroup(
            id = groupId,
            name = name,
            description = description,
            createdBy = userId,
            createdAt = java.util.Date(now),
            inviteLink = inviteLink,
            isActive = true,
            isPrivate = isPrivate,
            password = password,
            topic = topic
        )

        // Save to Firebase
        firebaseRepository.saveStudyGroup(group)

        // Save to Local Room
        studyGroupDao.insertGroup(group.toEntity())

        // Add creator as admin
        val member = StudyGroupMember(
            id = UUID.randomUUID().toString(),
            groupId = groupId,
            userId = userId,
            userName = firebaseRepository.getCurrentUser()?.name ?: "Admin",
            userEmail = firebaseRepository.getCurrentUser()?.email ?: "",
            joinedAt = java.util.Date(now),
            role = GroupMemberRole.ADMIN
        )
        
        firebaseRepository.joinStudyGroup(groupId, member)
        studyGroupDao.addMember(member.toEntity())

        Result.success(group)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getGroupById(groupId: String): StudyGroup? =
        studyGroupDao.getGroupById(groupId)?.toDomain()

    override suspend fun getGroupByInviteLink(inviteLink: String): StudyGroup? =
        studyGroupDao.getGroupByInviteLink(inviteLink)?.toDomain()

    override suspend fun getAllActiveGroups(): Flow<List<StudyGroup>> =
        firebaseRepository.getActiveStudyGroups()

    override suspend fun updateGroup(group: StudyGroup): Result<Unit> = try {
        firebaseRepository.saveStudyGroup(group)
        studyGroupDao.updateGroup(group.toEntity())
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun deleteGroup(groupId: String): Result<Unit> = try {
        val group = studyGroupDao.getGroupById(groupId)?.toDomain() 
            ?: firebaseRepository.getActiveStudyGroups().first().find { it.id == groupId }
        
        if (group != null) {
            val deletedGroup = group.copy(isActive = false)
            firebaseRepository.saveStudyGroup(deletedGroup)
            studyGroupDao.updateGroup(deletedGroup.toEntity())
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
        val member = StudyGroupMember(
            id = UUID.randomUUID().toString(),
            groupId = groupId,
            userId = userId,
            userName = userName,
            userEmail = userEmail,
            joinedAt = java.util.Date(),
            role = GroupMemberRole.MEMBER
        )
        firebaseRepository.joinStudyGroup(groupId, member)
        studyGroupDao.addMember(member.toEntity())
        Result.success(member)
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
        firebaseRepository.getGroupMembers(groupId)

    override suspend fun getUserGroups(userId: String): Flow<List<StudyGroup>> =
        studyGroupDao.getUserGroups(userId)
            .combine(firebaseRepository.getActiveStudyGroups()) { members, groups ->
                val memberGroupIds = members.map { it.groupId }.toSet()
                groups.filter { it.id in memberGroupIds }
            }

    override suspend fun isMemberOfGroup(groupId: String, userId: String): Boolean =
        studyGroupDao.getMember(groupId, userId) != null

    override suspend fun sendMessage(
        groupId: String,
        senderId: String,
        senderName: String,
        message: String
    ): Result<GroupMessage> = try {
        val groupMsg = GroupMessage(
            id = UUID.randomUUID().toString(),
            groupId = groupId,
            senderId = senderId,
            senderName = senderName,
            message = message,
            timestamp = java.util.Date(),
            isEdited = false
        )
        firebaseRepository.sendGroupMessage(groupId, groupMsg)
        studyGroupDao.insertMessage(groupMsg.toEntity())
        Result.success(groupMsg)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getGroupMessages(groupId: String): Flow<List<GroupMessage>> =
        firebaseRepository.getGroupMessages(groupId)

    override suspend fun editMessage(messageId: String, newContent: String): Result<Unit> = try {
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

    private fun StudyGroupMember.toEntity() = StudyGroupMemberEntity(
        id = id,
        groupId = groupId,
        userId = userId,
        userName = userName,
        userEmail = userEmail,
        joinedAt = joinedAt.time,
        role = role.name
    )

    private fun GroupMessage.toEntity() = GroupMessageEntity(
        id = id,
        groupId = groupId,
        senderId = senderId,
        senderName = senderName,
        message = message,
        timestamp = timestamp.time,
        isEdited = isEdited
    )
}
