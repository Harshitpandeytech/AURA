package com.aurviz.personalagent.domain.tools

/**
 * Common interface for all agent tools.
 * Every tool that the AI agent can invoke must implement this interface.
 */
interface AgentTool {
    /** Unique name used to identify the tool in function calls */
    val name: String

    /** Human-readable description of what the tool does */
    val description: String

    /** Whether this tool requires explicit user confirmation before execution */
    val requiresConfirmation: Boolean

    /** JSON schema describing the tool's parameters */
    val parameterSchema: ToolParameterSchema

    /**
     * Execute the tool with the given validated arguments.
     * @param arguments Map of parameter names to their values
     * @return ToolResult containing success/failure and output data
     */
    suspend fun execute(arguments: Map<String, String>): ToolResult

    /**
     * Validate the arguments before execution.
     * @return null if valid, error message if invalid
     */
    fun validateArguments(arguments: Map<String, String>): String?
}

interface ConfirmableTool : AgentTool {
    suspend fun confirm(): ToolResult
}

data class ToolParameterSchema(
    val parameters: List<ToolParameter>
)

data class ToolParameter(
    val name: String,
    val type: String,
    val description: String,
    val required: Boolean = true,
    val enumValues: List<String>? = null
)

data class ToolResult(
    val success: Boolean,
    val output: String,
    val data: Map<String, Any?> = emptyMap(),
    val error: String? = null
)
