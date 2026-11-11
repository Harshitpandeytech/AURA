package com.aurviz.personalagent.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurviz.personalagent.domain.model.ChatMessage
import com.aurviz.personalagent.domain.model.MessageRole
import com.aurviz.personalagent.domain.agent.AgentOrchestrator
import com.aurviz.personalagent.utils.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChatUiModel(
    val messages: List<ChatMessage> = emptyList(),
    val draft: String = "",
    val executionStatus: ExecutionStatus? = null,
    val isProcessing: Boolean = false
)

enum class ExecutionStatus(val label: String) {
    UNDERSTANDING("Understanding command..."),
    PLANNING("Planning task..."),
    EXECUTING("Executing action..."),
    COMPLETED("Completed.")
}

class ChatViewModel(
    private val agentOrchestrator: AgentOrchestrator? = null
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState<ChatUiModel>>(UiState.Loading)
    val uiState: StateFlow<UiState<ChatUiModel>> = _uiState.asStateFlow()

    private var latest = ChatUiModel()

    init {
        viewModelScope.launch {
            publish(UiState.Success(ChatUiModel()))
        }
    }

    fun onDraftChange(value: String) {
        val current = (_uiState.value as? UiState.Success)?.data ?: latest
        publish(UiState.Success(current.copy(draft = value)))
    }

    fun sendMessage() {
        val current = (_uiState.value as? UiState.Success)?.data ?: return
        if (current.isProcessing) return
        val text = current.draft.trim()
        if (text.isEmpty()) return

        val userMessage = ChatMessage(content = text, role = MessageRole.USER)
        val pending = current.copy(
            messages = current.messages + userMessage,
            draft = ""
        )
        publish(UiState.Success(pending.copy(isProcessing = true, executionStatus = ExecutionStatus.UNDERSTANDING)))

        viewModelScope.launch {
            try {
                publish(UiState.Success(latest.copy(isProcessing = true, executionStatus = ExecutionStatus.PLANNING)))
                publish(UiState.Success(latest.copy(isProcessing = true, executionStatus = ExecutionStatus.EXECUTING)))
                val reply = agentOrchestrator?.processCommand(text) ?: ChatMessage(
                    content = "AURA is set up. AI responses are not connected yet.",
                    role = MessageRole.ASSISTANT
                )
                publish(UiState.Success(latest.copy(
                    messages = latest.messages + reply,
                    isProcessing = false,
                    executionStatus = ExecutionStatus.COMPLETED
                )))
                kotlinx.coroutines.delay(1200)
                val complete = (_uiState.value as? UiState.Success)?.data ?: latest
                publish(UiState.Success(complete.copy(executionStatus = null)))
            } catch (error: Exception) {
                _uiState.value = UiState.Error(error.message ?: "Unable to send message")
            }
        }
    }

    fun setDraftFromVoice(value: String) = onDraftChange(value)

    fun confirmPendingAction() {
        val orchestrator = agentOrchestrator ?: return
        viewModelScope.launch {
            try {
                val reply = orchestrator.confirmPendingAction()
                val current = (_uiState.value as? UiState.Success)?.data ?: latest
                publish(UiState.Success(current.copy(messages = current.messages + reply)))
            } catch (error: Exception) {
                _uiState.value = UiState.Error(error.message ?: "Unable to confirm action")
            }
        }
    }

    fun dismissError() {
        _uiState.value = UiState.Success(latest)
    }

    private fun publish(state: UiState<ChatUiModel>) {
        if (state is UiState.Success) {
            latest = state.data
        }
        _uiState.value = state
    }
}
