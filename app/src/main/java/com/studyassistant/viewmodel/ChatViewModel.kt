package com.studyassistant.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyassistant.domain.model.ChatMessage
import com.studyassistant.domain.model.ChatRole
import com.studyassistant.repository.AIRepository
import com.studyassistant.repository.FirebaseRepository
import com.studyassistant.repository.LocalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val noteTitle: String = "",
    val noteContent: String = ""
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val aiRepository: AIRepository,
    private val firebaseRepository: FirebaseRepository,
    private val localRepository: LocalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var currentNoteId: String? = null

    fun loadNote(noteId: String) {
        currentNoteId = noteId
        val userId = firebaseRepository.getCurrentUser()?.id ?: return

        viewModelScope.launch {
            // 1. Fetch updated Note details from Cloud and cache locally
            launch {
                firebaseRepository.getNoteById(noteId).onSuccess { remoteNote ->
                    localRepository.cacheNote(remoteNote)
                }
            }

            // 2. Observe Note details from Local DB (Fast UI update)
            val note = localRepository.getCachedNoteById(noteId)
                ?: firebaseRepository.getNoteById(noteId).getOrNull()
            
            note?.let { n ->
                val content = buildString {
                    if (n.summary.isNotBlank()) append(n.summary).append("\n\n")
                    if (n.originalContent.isNotBlank()) append(n.originalContent)
                }.take(4000)
                _uiState.update { it.copy(noteTitle = n.title.ifBlank { "Note" }, noteContent = content) }
            }

            // 3. Real-time Cloud Sync: Fetch from Firebase and save to Room
            launch {
                firebaseRepository.getChatMessages(userId, noteId).collect { remoteMsgs ->
                    remoteMsgs.forEach { localRepository.saveChatMessage(userId, noteId, it) }
                }
            }

            // 4. UI Source of Truth: Observe local messages (works offline, updates when sync finishes)
            localRepository.getChatMessages(userId, noteId).collect { messages ->
                _uiState.update { it.copy(messages = messages) }
            }
        }
    }

    fun setInput(text: String) = _uiState.update { it.copy(inputText = text) }

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        val noteId = currentNoteId ?: return
        val userId = firebaseRepository.getCurrentUser()?.id ?: return
        if (text.isBlank() || _uiState.value.isLoading) return

        val userMsg = ChatMessage(id = UUID.randomUUID().toString(), role = ChatRole.USER, content = text)
        
        viewModelScope.launch {
            // Save user message locally and to Firebase
            localRepository.saveChatMessage(userId, noteId, userMsg)
            firebaseRepository.saveChatMessage(userId, noteId, userMsg)
            
            _uiState.update { it.copy(inputText = "", isLoading = true) }

            aiRepository.chatWithNote(
                noteContent = _uiState.value.noteContent,
                history = _uiState.value.messages,
                userMessage = text
            ).fold(
                onSuccess = { reply ->
                    val aiMsg = ChatMessage(id = UUID.randomUUID().toString(), role = ChatRole.AI, content = reply)
                    localRepository.saveChatMessage(userId, noteId, aiMsg)
                    firebaseRepository.saveChatMessage(userId, noteId, aiMsg)
                    _uiState.update { it.copy(isLoading = false) }
                },
                onFailure = {
                    _uiState.update { it.copy(isLoading = false) }
                }
            )
        }
    }
}
