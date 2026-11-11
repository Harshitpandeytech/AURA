package com.aurviz.personalagent.ai

import com.aurviz.personalagent.domain.model.ToolCallInfo
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class ToolCallParser(
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    fun parse(raw: String): List<ToolCallInfo> {
        if (raw.isBlank()) return emptyList()
        return try {
            json.decodeFromString<List<RawToolCall>>(raw).map { call ->
                ToolCallInfo(
                    toolName = call.name,
                    arguments = call.arguments
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}

@Serializable
internal data class RawToolCall(
    val name: String,
    val arguments: Map<String, String> = emptyMap()
)
