package com.studyassistant.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyassistant.domain.model.GroupMessage
import com.studyassistant.domain.model.StudyGroup
import com.studyassistant.domain.model.StudyGroupMember
import com.studyassistant.repository.StudyGroupRepository
import com.studyassistant.repository.FirebaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StudyGroupsUiState(
    val userGroups: List<StudyGroup> = emptyList(),
    val publicGroups: List<StudyGroup> = emptyList(),
    val selectedGroup: StudyGroup? = null,
    val groupMembers: List<StudyGroupMember> = emptyList(),
    val groupMessages: List<GroupMessage> = emptyList(),
    val inviteInput: String = "",
    val passwordInput: String = "",
    val messageInput: String = "",
    val isLoading: Boolean = false,
    val isCreating: Boolean = false,
    val error: String? = null,
    val inviteLink: String? = null
)

@HiltViewModel
class StudyGroupViewModel @Inject constructor(
    private val studyGroupRepository: StudyGroupRepository,
    private val firebaseRepository: FirebaseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudyGroupsUiState())
    val uiState: StateFlow<StudyGroupsUiState> = _uiState.asStateFlow()

    init {
        loadUserGroups()
        loadPublicGroups()
    }

    private fun loadUserGroups() {
        viewModelScope.launch {
            try {
                val userId = firebaseRepository.getCurrentUser()?.id ?: return@launch
                studyGroupRepository.getUserGroups(userId).collect { groups ->
                    val joinedIds = groups.map { it.id }.toSet()
                    _uiState.update {
                        it.copy(
                            userGroups = groups,
                            publicGroups = it.publicGroups.filter { group -> group.id !in joinedIds },
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to load groups", isLoading = false) }
            }
        }
    }

    private fun loadPublicGroups() {
        viewModelScope.launch {
            try {
                studyGroupRepository.getAllActiveGroups().collect { groups ->
                    val joinedIds = _uiState.value.userGroups.map { it.id }.toSet()
                    _uiState.update {
                        it.copy(publicGroups = groups.filter { group -> !group.isPrivate && group.id !in joinedIds })
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to load public groups") }
            }
        }
    }

    fun createGroup(name: String, description: String, topic: String, isPrivate: Boolean, password: String) {
        viewModelScope.launch {
            try {
                val userId = firebaseRepository.getCurrentUser()?.id ?: run {
                    _uiState.update { it.copy(error = "Not signed in") }
                    return@launch
                }

                _uiState.update { it.copy(isCreating = true, error = null) }
                val result = studyGroupRepository.createGroup(userId, name, description, topic, isPrivate, password)
                result.fold(
                    onSuccess = { group ->
                        _uiState.update { it.copy(
                            selectedGroup = group,
                            inviteLink = group.inviteLink,
                            isCreating = false
                        )}
                        loadUserGroups()
                    },
                    onFailure = { e ->
                        _uiState.update { it.copy(
                            error = e.message ?: "Failed to create group",
                            isCreating = false
                        )}
                    }
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    error = e.message ?: "Failed to create group",
                    isCreating = false
                )}
            }
        }
    }

    fun selectGroup(group: StudyGroup) {
        _uiState.update { it.copy(selectedGroup = group) }
        loadGroupMembers(group.id)
        loadGroupMessages(group.id)
    }

    fun openGroup(groupId: String) {
        viewModelScope.launch {
            try {
                val group = studyGroupRepository.getGroupById(groupId) ?: run {
                    _uiState.update { it.copy(error = "Group not found") }
                    return@launch
                }
                selectGroup(group)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to open group") }
            }
        }
    }

    private fun loadGroupMembers(groupId: String) {
        viewModelScope.launch {
            try {
                studyGroupRepository.getGroupMembers(groupId).collect { members ->
                    _uiState.update { it.copy(groupMembers = members) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to load members") }
            }
        }
    }

    private fun loadGroupMessages(groupId: String) {
        viewModelScope.launch {
            try {
                studyGroupRepository.getGroupMessages(groupId).collect { messages ->
                    _uiState.update { it.copy(groupMessages = messages.reversed()) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to load messages") }
            }
        }
    }

    fun updateInviteInput(text: String) {
        _uiState.update { it.copy(inviteInput = text) }
    }

    fun updatePasswordInput(text: String) {
        _uiState.update { it.copy(passwordInput = text) }
    }

    fun joinPublicGroup(group: StudyGroup) {
        joinGroup(group.inviteLink, "")
    }

    fun joinGroup(inviteLink: String = _uiState.value.inviteInput, password: String = _uiState.value.passwordInput) {
        viewModelScope.launch {
            try {
                val userId = firebaseRepository.getCurrentUser()?.id ?: run {
                    _uiState.update { it.copy(error = "Not signed in") }
                    return@launch
                }
                val user = firebaseRepository.getCurrentUser() ?: return@launch

                val group = studyGroupRepository.getGroupByInviteLink(inviteLink) ?: run {
                    _uiState.update { it.copy(error = "Group not found") }
                    return@launch
                }

                if (group.isPrivate && group.password.isNotBlank() && group.password != password) {
                    _uiState.update { it.copy(error = "Incorrect group password") }
                    return@launch
                }

                val result = studyGroupRepository.addMember(group.id, userId, user.name, user.email)
                result.fold(
                    onSuccess = {
                        _uiState.update { it.copy(error = null, inviteInput = "", passwordInput = "") }
                        loadUserGroups()
                    },
                    onFailure = { e ->
                        _uiState.update { it.copy(error = e.message ?: "Failed to join group") }
                    }
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to join group") }
            }
        }
    }

    fun sendMessage(messageText: String) {
        viewModelScope.launch {
            try {
                val userId = firebaseRepository.getCurrentUser()?.id ?: return@launch
                val userName = firebaseRepository.getCurrentUser()?.name ?: "Anonymous"
                val groupId = _uiState.value.selectedGroup?.id ?: return@launch

                val result = studyGroupRepository.sendMessage(groupId, userId, userName, messageText)
                result.fold(
                    onSuccess = {
                        _uiState.update { it.copy(messageInput = "") }
                        loadGroupMessages(groupId)
                    },
                    onFailure = { e ->
                        _uiState.update { it.copy(error = e.message ?: "Failed to send message") }
                    }
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to send message") }
            }
        }
    }

    fun leaveGroup(groupId: String) {
        viewModelScope.launch {
            try {
                val userId = firebaseRepository.getCurrentUser()?.id ?: return@launch
                val result = studyGroupRepository.removeMember(groupId, userId)
                result.fold(
                    onSuccess = {
                        _uiState.update { it.copy(selectedGroup = null) }
                        loadUserGroups()
                    },
                    onFailure = { e ->
                        _uiState.update { it.copy(error = e.message ?: "Failed to leave group") }
                    }
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to leave group") }
            }
        }
    }

    fun updateMessageInput(text: String) {
        _uiState.update { it.copy(messageInput = text) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
