package com.studyassistant.data.local.dao

import androidx.room.*
import com.studyassistant.data.local.entity.GroupMessageEntity
import com.studyassistant.data.local.entity.StudyGroupEntity
import com.studyassistant.data.local.entity.StudyGroupMemberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyGroupDao {
    // Group operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: StudyGroupEntity)

    @Query("SELECT * FROM study_groups WHERE id = :groupId")
    suspend fun getGroupById(groupId: String): StudyGroupEntity?

    @Query("SELECT * FROM study_groups WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getAllActiveGroups(): Flow<List<StudyGroupEntity>>

    @Query("SELECT * FROM study_groups WHERE inviteLink = :inviteLink LIMIT 1")
    suspend fun getGroupByInviteLink(inviteLink: String): StudyGroupEntity?

    @Update
    suspend fun updateGroup(group: StudyGroupEntity)

    @Delete
    suspend fun deleteGroup(group: StudyGroupEntity)

    // Member operations
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addMember(member: StudyGroupMemberEntity)

    @Query("SELECT * FROM study_group_members WHERE groupId = :groupId ORDER BY joinedAt DESC")
    fun getGroupMembers(groupId: String): Flow<List<StudyGroupMemberEntity>>

    @Query("SELECT * FROM study_group_members WHERE groupId = :groupId AND userId = :userId LIMIT 1")
    suspend fun getMember(groupId: String, userId: String): StudyGroupMemberEntity?

    @Query("SELECT COUNT(*) FROM study_group_members WHERE groupId = :groupId")
    fun getMemberCount(groupId: String): Flow<Int>

    @Query("DELETE FROM study_group_members WHERE groupId = :groupId AND userId = :userId")
    suspend fun removeMember(groupId: String, userId: String)

    @Query("SELECT * FROM study_group_members WHERE userId = :userId ORDER BY joinedAt DESC")
    fun getUserGroups(userId: String): Flow<List<StudyGroupMemberEntity>>

    // Message operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: GroupMessageEntity)

    @Query("SELECT * FROM group_messages WHERE groupId = :groupId ORDER BY timestamp DESC LIMIT 50")
    fun getGroupMessages(groupId: String): Flow<List<GroupMessageEntity>>

    @Update
    suspend fun updateMessage(message: GroupMessageEntity)

    @Delete
    suspend fun deleteMessage(message: GroupMessageEntity)
}
