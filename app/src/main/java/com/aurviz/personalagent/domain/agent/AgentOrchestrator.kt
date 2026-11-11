package com.aurviz.personalagent.domain.agent

import com.aurviz.personalagent.ai.GeminiAgent
import com.aurviz.personalagent.ai.PromptBuilder
import com.aurviz.personalagent.ai.toolDeclarations
import com.aurviz.personalagent.domain.model.ChatMessage
import com.aurviz.personalagent.domain.model.MessageRole
import com.aurviz.personalagent.domain.repository.MemoryRepository
import com.aurviz.personalagent.domain.tools.ToolRegistry
import com.aurviz.personalagent.domain.tools.ToolResult
import com.aurviz.personalagent.domain.tools.ConfirmableTool
import kotlinx.serialization.json.*
import java.util.concurrent.atomic.AtomicBoolean

interface AgentOrchestrator {
    suspend fun processCommand(userMessage: String): ChatMessage
    suspend fun confirmPendingAction(): ChatMessage
    fun isProcessing(): Boolean
}

class DefaultAgentOrchestrator(
    private val memoryRepository: MemoryRepository,
    private val geminiAgent: GeminiAgent,
    private val promptBuilder: PromptBuilder,
    private val toolRegistry: ToolRegistry,
    private val maxToolCalls: Int = 5
) : AgentOrchestrator {
    private val processing = AtomicBoolean(false)

    override fun isProcessing(): Boolean = processing.get()

    override suspend fun confirmPendingAction(): ChatMessage {
        val tool = toolRegistry.getAllTools().filterIsInstance<ConfirmableTool>().firstOrNull()
            ?: return ChatMessage(
                content = "No action is awaiting confirmation.",
                role = MessageRole.ASSISTANT
            )
        val result = tool.confirm()
        return ChatMessage(
            content = if (result.success) result.output else result.error ?: "Confirmation failed.",
            role = MessageRole.ASSISTANT
        )
    }

    override suspend fun processCommand(userMessage: String): ChatMessage {
        require(userMessage.isNotBlank()) { "Command cannot be blank" }
        check(processing.compareAndSet(false, true)) { "Agent is already processing a command" }
        try {
            val memories = memoryRepository.searchMemories(userMessage)
            val contents = mutableListOf(
                buildJsonObject {
                    put("role", "user")
                    put("parts", JsonArray(listOf(JsonObject(mapOf(
                        "text" to JsonPrimitive(promptBuilder.build(userMessage, memories))
                    )))))
                }
            )
            val declarations = toolDeclarations(toolRegistry.getAllTools())
            var executed = 0
            while (true) {
                val response = geminiAgent.generate(contents, declarations)
                if (response.functionCalls.isEmpty()) {
                    return ChatMessage(content = response.text ?: "Gemini returned an empty response.", role = MessageRole.ASSISTANT)
                }
                if (executed + response.functionCalls.size > maxToolCalls) {
                    return ChatMessage(content = "I stopped because the tool-call limit was reached.", role = MessageRole.ASSISTANT)
                }
                contents += response.modelContent
                var confirmationMessage: ChatMessage? = null
                val resultParts = response.functionCalls.map { call ->
                    executed++
                    val tool = toolRegistry.getTool(call.name)
                    val result = when {
                        tool == null -> ToolResult(false, "", error = "Unknown tool: ${call.name}")
                        else -> tool.validateArguments(call.arguments)?.let { "Invalid arguments: $it" }
                            ?.let { ToolResult(false, "", error = it) }
                            ?: runCatching { tool.execute(call.arguments) }
                                .getOrElse { ToolResult(false, "", error = it.message ?: "Tool execution failed") }
                    }
                    if (tool?.requiresConfirmation == true && result.success) {
                        confirmationMessage = ChatMessage(
                            content = result.output,
                            role = MessageRole.ASSISTANT,
                            toolCalls = listOf(
                                com.aurviz.personalagent.domain.model.ToolCallInfo(
                                    toolName = call.name,
                                    arguments = call.arguments,
                                    status = com.aurviz.personalagent.domain.model.ToolCallStatus.AWAITING_CONFIRMATION,
                                    result = result.output
                                )
                            )
                        )
                    }
                    JsonObject(mapOf(
                        "functionResponse" to JsonObject(mapOf(
                            "name" to JsonPrimitive(call.name),
                            "response" to JsonObject(mapOf(
                                "result" to JsonPrimitive(if (result.success) result.output else result.error ?: "Tool failed")
                            ))
                        ))
                    ))
                }
                confirmationMessage?.let { return it }
                contents += JsonObject(mapOf("role" to JsonPrimitive("user"), "parts" to JsonArray(resultParts)))
            }
        } finally {
            processing.set(false)
        }
    }
}
