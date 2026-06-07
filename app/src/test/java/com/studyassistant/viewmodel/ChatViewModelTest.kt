package com.studyassistant.viewmodel

import com.google.common.truth.Truth.assertThat
import com.studyassistant.domain.model.Note
import com.studyassistant.domain.model.User
import com.studyassistant.repository.AIRepository
import com.studyassistant.repository.FirebaseRepository
import com.studyassistant.repository.LocalRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val aiRepository: AIRepository = mock()
    private val firebaseRepository: FirebaseRepository = mock()
    private val localRepository: LocalRepository = mock()

    private lateinit var viewModel: ChatViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // Mock a user (ChatViewModel returns early if no user is found)
        val dummyUser = User(id = "user_123")
        whenever(firebaseRepository.getCurrentUser()).thenReturn(dummyUser)

        // Mock a default empty message flow
        whenever(localRepository.getChatMessages(any(), any())).thenReturn(flowOf(emptyList()))

        viewModel = ChatViewModel(aiRepository, firebaseRepository, localRepository)
    }

    @Test
    fun `setInput updates the inputText in state`() {
        viewModel.setInput("Hello AI")
        assertThat(viewModel.uiState.value.inputText).isEqualTo("Hello AI")
    }

    @Test
    fun `sendMessage clears input and calls AI repository`() = runTest {
        // ARRANGE: Set up a note so sendMessage doesn't return early
        val noteId = "test_note"
        val note = Note(id = noteId, title = "Test Note", originalContent = "Content")
        whenever(localRepository.getCachedNoteById(noteId)).thenReturn(note)

        viewModel.loadNote(noteId)
        viewModel.setInput("What is Android?")

        whenever(aiRepository.chatWithNote(any(), any(), any()))
            .thenReturn(Result.success("Android is an OS"))

        // ACT
        viewModel.sendMessage()

        // advance dispatcher so launched coroutines run
        testDispatcher.scheduler.advanceUntilIdle()

        // ASSERT
        assertThat(viewModel.uiState.value.inputText).isEmpty()
        verify(aiRepository).chatWithNote(any(), any(), eq("What is Android?"))
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }
}
