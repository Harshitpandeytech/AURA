package com.aurviz.personalagent.presentation.chat

import com.aurviz.personalagent.MainDispatcherRule
import com.aurviz.personalagent.domain.model.MessageRole
import com.aurviz.personalagent.utils.UiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ChatViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun startsReadyForInput() {
        val viewModel = ChatViewModel()
        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        assertTrue((state as UiState.Success).data.messages.isEmpty())
    }

    @Test
    fun sendMessage_keepsUserTextAndLocalReply() {
        val viewModel = ChatViewModel()
        viewModel.onDraftChange("Hello")
        viewModel.sendMessage()

        val state = viewModel.uiState.value as UiState.Success
        assertEquals("", state.data.draft)
        assertEquals(2, state.data.messages.size)
        assertEquals(MessageRole.USER, state.data.messages[0].role)
        assertEquals("Hello", state.data.messages[0].content)
        assertEquals(MessageRole.ASSISTANT, state.data.messages[1].role)
    }

    @Test
    fun blankDraft_doesNotAppendMessages() {
        val viewModel = ChatViewModel()
        viewModel.onDraftChange("   ")
        viewModel.sendMessage()
        val state = viewModel.uiState.value as UiState.Success
        assertTrue(state.data.messages.isEmpty())
    }
}
