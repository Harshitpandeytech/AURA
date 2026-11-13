package com.aurviz.personalagent.domain.agent

import com.aurviz.personalagent.ai.GeminiAgent
import com.aurviz.personalagent.ai.PromptBuilder
import com.aurviz.personalagent.ai.toolDeclarations
import com.aurviz.personalagent.domain.model.ChatMessage
import com.aurviz.personalagent.domain.model.ExecutionLog
import com.aurviz.personalagent.domain.model.ExecutionStatus
import com.aurviz.personalagent.domain.model.MessageRole
import com.aurviz.personalagent.domain.repository.ExecutionLogRepository
import com.aurviz.personalagent.domain.repository.ConversationContextRepository
import com.aurviz.personalagent.domain.repository.MemoryRepository
import com.aurviz.personalagent.domain.tools.ToolRegistry
import com.aurviz.personalagent.domain.tools.ToolResult
import com.aurviz.personalagent.domain.tools.ConfirmableTool
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.*
import java.util.concurrent.atomic.AtomicBoolean

interface AgentOrchestrator {
    suspend fun processCommand(
        userMessage: String,
        recentMessages: List<ChatMessage> = emptyList()
    ): ChatMessage
    suspend fun confirmPendingAction(): ChatMessage
    suspend fun clearConversationContext()
    fun isProcessing(): Boolean
}

class DefaultAgentOrchestrator(
    private val memoryRepository: MemoryRepository,
    private val geminiAgent: GeminiAgent,
    private val promptBuilder: PromptBuilder,
    private val toolRegistry: ToolRegistry,
    private val executionLogRepository: ExecutionLogRepository? = null,
    private val conversationContextRepository: ConversationContextRepository? = null,
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
        val startTime = System.currentTimeMillis()
        val result = tool.confirm()
        val duration = System.currentTimeMillis() - startTime
        executionLogRepository?.insertLog(
            ExecutionLog(
                taskId = "confirm_${tool.name}",
                command = "Confirm action: ${tool.name}",
                status = if (result.success) ExecutionStatus.SUCCESS else ExecutionStatus.FAILURE,
                result = if (result.success) result.output else result.error ?: "Confirmation failed",
                durationMs = duration
            )
        )
        return ChatMessage(
            content = if (result.success) result.output else result.error ?: "Confirmation failed.",
            role = MessageRole.ASSISTANT
        )
    }

    override suspend fun clearConversationContext() {
        conversationContextRepository?.clear()
    }

    override suspend fun processCommand(
        userMessage: String,
        recentMessages: List<ChatMessage>
    ): ChatMessage {
        require(userMessage.isNotBlank()) { "Command cannot be blank" }
        check(processing.compareAndSet(false, true)) { "Agent is already processing a command" }
        val startTime = System.currentTimeMillis()
        try {
            val savedContext = conversationContextRepository?.get().orEmpty()
            val memories = relevantMemories(userMessage, recentMessages)
            val contents = mutableListOf(
                buildJsonObject {
                    put("role", "user")
                    put("parts", JsonArray(listOf(JsonObject(mapOf(
                        "text" to JsonPrimitive(
                            promptBuilder.build(
                                userMessage = userMessage,
                                memories = memories,
                                recentMessages = recentMessages,
                                conversationSummary = savedContext
                            )
                        )
                    )))))
                }
            )
            val declarations = toolDeclarations(toolRegistry.getAllTools())
            var executed = 0
            while (true) {
                val response = geminiAgent.generate(contents, declarations)
                if (response.functionCalls.isEmpty()) {
                    val finalReply = response.text ?: "Gemini returned an empty response."
                    executionLogRepository?.insertLog(
                        ExecutionLog(
                            taskId = "chat_${System.currentTimeMillis()}",
                            command = userMessage,
                            status = ExecutionStatus.SUCCESS,
                            result = finalReply,
                            durationMs = System.currentTimeMillis() - startTime
                        )
                    )
                    saveConversationContext(savedContext, userMessage, finalReply)
                    return ChatMessage(content = finalReply, role = MessageRole.ASSISTANT)
                }
                if (executed + response.functionCalls.size > maxToolCalls) {
                    val limitMsg = "I stopped because the tool-call limit was reached."
                    executionLogRepository?.insertLog(
                        ExecutionLog(
                            taskId = "chat_${System.currentTimeMillis()}",
                            command = userMessage,
                            status = ExecutionStatus.CANCELLED,
                            result = limitMsg,
                            durationMs = System.currentTimeMillis() - startTime
                        )
                    )
                    return ChatMessage(content = limitMsg, role = MessageRole.ASSISTANT)
                }
                contents += response.modelContent
                var confirmationMessage: ChatMessage? = null
                val resultParts = response.functionCalls.map { call ->
                    executed++
                    val tool = toolRegistry.getTool(call.name)
                    val toolStartTime = System.currentTimeMillis()
                    val result = when {
                        tool == null -> ToolResult(false, "", error = "Unknown tool: ${call.name}")
                        else -> tool.validateArguments(call.arguments)?.let { "Invalid arguments: $it" }
                            ?.let { ToolResult(false, "", error = it) }
                            ?: runCatching { tool.execute(call.arguments) }
                                .getOrElse { ToolResult(false, "", error = it.message ?: "Tool execution failed") }
                    }
                    val toolDuration = System.currentTimeMillis() - toolStartTime

                    executionLogRepository?.insertLog(
                        ExecutionLog(
                            taskId = "tool_${call.name}",
                            command = "${call.name}: ${call.arguments}",
                            status = if (result.success) ExecutionStatus.SUCCESS else ExecutionStatus.FAILURE,
                            result = if (result.success) result.output else result.error ?: "Tool failed",
                            durationMs = toolDuration
                        )
                    )

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
                                "success" to JsonPrimitive(result.success),
                                "output" to JsonPrimitive(
                                    if (result.success) result.output else result.error ?: "Tool failed"
                                ),
                                "result" to JsonPrimitive(if (result.success) result.output else result.error ?: "Tool failed")
                            ))
                        ))
                    ))
                }
                confirmationMessage?.let { return it }
                contents += JsonObject(mapOf("role" to JsonPrimitive("user"), "parts" to JsonArray(resultParts)))
            }
        } catch (e: Exception) {
            executionLogRepository?.insertLog(
                ExecutionLog(
                    taskId = "chat_${System.currentTimeMillis()}",
                    command = userMessage,
                    status = ExecutionStatus.FAILURE,
                    result = e.message ?: "Execution error",
                    durationMs = System.currentTimeMillis() - startTime
                )
            )
            throw e
        } finally {
            processing.set(false)
        }
    }

        private suspend fun relevantMemories(
            userMessage: String,
            recentMessages: List<ChatMessage>
        ): List<com.aurviz.personalagent.domain.model.Memory> {
            val queryWords = (userMessage + " " + recentMessages.takeLast(4).joinToString(" ") { it.content })
                .lowercase()
                .split(Regex("[^a-z0-9@._-]+"))
                .filter { it.length >= 3 && it !in MEMORY_STOP_WORDS }
                .distinct()
                .take(MAX_MEMORY_QUERIES)
            if (queryWords.isEmpty()) return emptyList()
            val matches = queryWords
                .flatMap { memoryRepository.searchMemories(it) }
            return matches
                .groupBy { it.key }
                .values
                .map { candidates ->
                    val memory = candidates.maxBy { it.updatedAt }
                    val searchable = "${memory.key} ${memory.value} ${memory.category}".lowercase()
                    val relevance = queryWords.count { searchable.contains(it) } +
                        if (queryWords.any { it == memory.key.lowercase() }) KEY_MATCH_BONUS else 0
                    relevance to memory
                }
                .sortedWith(
                    compareByDescending<Pair<Int, com.aurviz.personalagent.domain.model.Memory>> { it.first }
                        .thenByDescending { it.second.updatedAt }
                )
                .map { it.second }
                .take(MAX_RELEVANT_MEMORIES)
        }

        private suspend fun saveConversationContext(
            existing: String,
            userMessage: String,
            assistantReply: String
        ) {
            val updated = listOf(
                existing,
                "User: $userMessage",
                "AURA: $assistantReply"
            ).filter { it.isNotBlank() }.joinToString("\n")
            conversationContextRepository?.save(
                updated.takeLast(MAX_CONTEXT_CHARS)
            )
        }

        private companion object {
            const val MAX_MEMORY_QUERIES = 8
            const val MAX_RELEVANT_MEMORIES = 12
            const val MAX_CONTEXT_CHARS = 6_000
            const val KEY_MATCH_BONUS = 2
            val MEMORY_STOP_WORDS = setOf(
                "and", "are", "can", "for", "from", "how", "that", "the", "this",
                "what", "when", "where", "which", "who", "with", "you", "your"
            )
        }
    }
