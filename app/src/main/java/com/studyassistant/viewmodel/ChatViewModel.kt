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

    fun loadNote(noteId: String) {
        viewModelScope.launch {
            val note = localRepository.getCachedNoteById(noteId)
                ?: firebaseRepository.getNoteById(noteId).getOrNull()
                ?: return@launch
            val content = buildString {
                if (note.summary.isNotBlank()) append(note.summary).append("\n\n")
                if (note.originalContent.isNotBlank()) append(note.originalContent)
            }.take(4000)
            _uiState.update { it.copy(noteTitle = note.title.ifBlank { "Note" }, noteContent = content) }

            // Welcome message
            _uiState.update { state ->
                state.copy(messages = listOf(ChatMessage(
                    id = UUID.randomUUID().toString(),
                    role = ChatRole.AI,
                    content = "Hi! I've read \"${note.title.ifBlank { "your note" }}\". Ask me anything about it — I can explain concepts, quiz you, or help clarify anything."
                )))
            }
        }
    }

    fun setInput(text: String) = _uiState.update { it.copy(inputText = text) }

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isBlank() || _uiState.value.isLoading) return

        val userMsg = ChatMessage(id = UUID.randomUUID().toString(), role = ChatRole.USER, content = text)
        _uiState.update { it.copy(messages = it.messages + userMsg, inputText = "", isLoading = true) }

        viewModelScope.launch {
            aiRepository.chatWithNote(
                noteContent = _uiState.value.noteContent,
                history = _uiState.value.messages.dropLast(0),
                userMessage = text
            ).fold(
                onSuccess = { reply ->
                    val aiMsg = ChatMessage(id = UUID.randomUUID().toString(), role = ChatRole.AI, content = reply)
                    _uiState.update { it.copy(messages = it.messages + aiMsg, isLoading = false) }
                },
                onFailure = {
                    val errMsg = ChatMessage(id = UUID.randomUUID().toString(), role = ChatRole.AI,
                        content = "Sorry, I couldn't connect. Please try again.")
                    _uiState.update { it.copy(messages = it.messages + errMsg, isLoading = false) }
                }
            )
        }
    }
}
