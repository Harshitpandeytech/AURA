package com.aurviz.personalagent.tools

import com.aurviz.personalagent.domain.tools.AgentTool
import com.aurviz.personalagent.domain.tools.ToolParameter
import com.aurviz.personalagent.domain.tools.ToolParameterSchema
import com.aurviz.personalagent.domain.tools.ToolResult

abstract class UnavailableTool(
    override val name: String,
    override val description: String,
    parameters: List<ToolParameter>
) : AgentTool {
    override val requiresConfirmation: Boolean = true
    override val parameterSchema: ToolParameterSchema = ToolParameterSchema(parameters)

    override suspend fun execute(arguments: Map<String, String>): ToolResult {
        return ToolResult(
            success = false,
            output = "",
            error = "$name is not available yet"
        )
    }

    override fun validateArguments(arguments: Map<String, String>): String? {
        val missing = parameterSchema.parameters.filter { parameter ->
            parameter.required && arguments[parameter.name].isNullOrBlank()
        }
        if (missing.isEmpty()) return null
        return "Missing: ${missing.joinToString { it.name }}"
    }
}
