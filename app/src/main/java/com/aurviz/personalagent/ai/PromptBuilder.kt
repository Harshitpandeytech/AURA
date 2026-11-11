package com.aurviz.personalagent.ai

import com.aurviz.personalagent.domain.model.Memory

class PromptBuilder {
    val systemInstruction = """
        You are AURA, a careful personal assistant. Use saved memories only when relevant.
        Use memory tools to save, search, update, or delete user facts when requested.
        Never claim a tool succeeded unless its result says it succeeded. Be concise and truthful.
    """.trimIndent()

    fun build(userMessage: String, memories: List<Memory> = emptyList()): String {
        val memoryBlock = if (memories.isEmpty()) {
            "No saved memories."
        } else {
            memories.joinToString(separator = "\n") { memory ->
                "- ${memory.key}: ${memory.value}"
            }
        }
        return """
            $systemInstruction
            Memories:
            $memoryBlock
            User:
            $userMessage
        """.trimIndent()
    }
}
