package com.aurviz.personalagent.tools.memory

import com.aurviz.personalagent.domain.tools.ToolParameter
import com.aurviz.personalagent.domain.tools.AgentTool
import com.aurviz.personalagent.domain.tools.ToolParameterSchema
import com.aurviz.personalagent.domain.tools.ToolResult
import com.aurviz.personalagent.domain.model.Memory
import com.aurviz.personalagent.domain.repository.MemoryRepository

class MemoryTool(private val repository: MemoryRepository) : AgentTool {
    override val name = "save_memory"
    override val description = "Save or replace a personal memory."
    override val requiresConfirmation = false
    override val parameterSchema = ToolParameterSchema(
        listOf(
            ToolParameter("key", "string", "Stable memory key"),
            ToolParameter("value", "string", "Memory value"),
            ToolParameter("category", "string", "Memory category", required = false)
        )
    )

    override suspend fun execute(arguments: Map<String, String>): ToolResult {
        val key = arguments["key"]?.trim().orEmpty()
        val value = arguments["value"]?.trim().orEmpty()
        val category = arguments["category"]?.trim().takeUnless { it.isNullOrEmpty() } ?: "general"
        val existing = repository.getMemoryByKey(key)
        val now = System.currentTimeMillis()
        val memory = Memory(
            id = existing?.id ?: 0,
            key = key,
            value = value,
            category = category,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now
        )
        if (existing == null) repository.saveMemory(memory) else repository.updateMemory(memory)
        return ToolResult(true, "Saved memory '$key'.", mapOf("key" to key))
    }

    override fun validateArguments(arguments: Map<String, String>): String? =
        missing(arguments, "key", "value")
}

class SearchMemoryTool(private val repository: MemoryRepository) : AgentTool {
    override val name = "search_memory"
    override val description = "Find saved memories matching a keyword."
    override val requiresConfirmation = false
    override val parameterSchema = ToolParameterSchema(
        listOf(ToolParameter("query", "string", "Keyword to search for"))
    )

    override suspend fun execute(arguments: Map<String, String>): ToolResult {
        val memories = repository.searchMemories(arguments.getValue("query"))
        val output = memories.joinToString("\n") { "${it.key}: ${it.value} (${it.category})" }
            .ifBlank { "No matching memories found." }
        return ToolResult(true, output, mapOf("count" to memories.size))
    }

    override fun validateArguments(arguments: Map<String, String>): String? = missing(arguments, "query")
}

class UpdateMemoryTool(private val repository: MemoryRepository) : AgentTool {
    override val name = "update_memory"
    override val description = "Update an existing personal memory."
    override val requiresConfirmation = false
    override val parameterSchema = ToolParameterSchema(
        listOf(
            ToolParameter("key", "string", "Existing memory key"),
            ToolParameter("value", "string", "New memory value"),
            ToolParameter("category", "string", "New category", required = false)
        )
    )

    override suspend fun execute(arguments: Map<String, String>): ToolResult {
        val key = arguments.getValue("key")
        val existing = repository.getMemoryByKey(key)
            ?: return ToolResult(false, "", error = "Memory '$key' was not found.")
        val now = System.currentTimeMillis()
        repository.updateMemory(
            existing.copy(
                value = arguments.getValue("value"),
                category = arguments["category"]?.takeUnless { it.isBlank() } ?: existing.category,
                updatedAt = now
            )
        )
        return ToolResult(true, "Updated memory '$key'.")
    }

    override fun validateArguments(arguments: Map<String, String>): String? = missing(arguments, "key", "value")
}

class DeleteMemoryTool(private val repository: MemoryRepository) : AgentTool {
    override val name = "delete_memory"
    override val description = "Delete a saved personal memory by key."
    override val requiresConfirmation = false
    override val parameterSchema = ToolParameterSchema(
        listOf(ToolParameter("key", "string", "Memory key"))
    )

    override suspend fun execute(arguments: Map<String, String>): ToolResult {
        val key = arguments.getValue("key")
        val existing = repository.getMemoryByKey(key)
            ?: return ToolResult(false, "", error = "Memory '$key' was not found.")
        repository.deleteMemory(existing.id)
        return ToolResult(true, "Deleted memory '$key'.")
    }

    override fun validateArguments(arguments: Map<String, String>): String? = missing(arguments, "key")
}

private fun missing(arguments: Map<String, String>, vararg names: String): String? {
    val missing = names.filter { arguments[it].isNullOrBlank() }
    return missing.takeIf { it.isNotEmpty() }?.let { "Missing required arguments: ${it.joinToString()}" }
}
