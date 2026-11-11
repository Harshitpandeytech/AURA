package com.aurviz.personalagent.domain.tools

import com.aurviz.personalagent.utils.Logger

/**
 * Registry for all available agent tools.
 * Tools register themselves here and the orchestrator looks them up by name.
 */
class ToolRegistry {
    private val tools = mutableMapOf<String, AgentTool>()

    fun register(tool: AgentTool) {
        Logger.i("ToolRegistry", "Registering tool: ${tool.name}")
        tools[tool.name] = tool
    }

    fun getTool(name: String): AgentTool? = tools[name]

    fun getAllTools(): List<AgentTool> = tools.values.toList()

    fun getToolNames(): List<String> = tools.keys.toList()

    fun isToolRegistered(name: String): Boolean = tools.containsKey(name)

    fun unregister(name: String) {
        tools.remove(name)
    }
}
