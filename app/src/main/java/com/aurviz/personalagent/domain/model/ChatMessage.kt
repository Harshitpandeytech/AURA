package com.aurviz.personalagent.domain.model

import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val role: MessageRole,
    val timestamp: Long = System.currentTimeMillis(),
    val toolCalls: List<ToolCallInfo> = emptyList(),
    val isLoading: Boolean = false
)

enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM,
    TOOL_RESULT
}

data class ToolCallInfo(
    val toolName: String,
    val arguments: Map<String, String> = emptyMap(),
    val status: ToolCallStatus = ToolCallStatus.PENDING,
    val result: String = ""
)

enum class ToolCallStatus {
    PENDING,
    EXECUTING,
    SUCCESS,
    FAILURE,
    AWAITING_CONFIRMATION
}
