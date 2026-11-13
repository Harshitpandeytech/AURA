package com.aurviz.personalagent.ai

import com.aurviz.personalagent.data.remote.BackendClient
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class BackendAgent(private val client: BackendClient) {
    val isEnabled: Boolean
        get() = client.isConfigured && client.isAuthenticated

    suspend fun generate(contents: List<JsonObject>, tools: JsonArray): GeminiResponse {
        require(client.isAuthenticated) {
            "Sign in to the AURA backend before using the production AI service."
        }
        return parseProviderResponse(client.generate(contents, tools))
    }

    private fun parseProviderResponse(root: JsonObject): GeminiResponse {
        if (root["candidates"] != null) {
            return parseGeminiResponse(root)
        }
        val message = root["choices"]?.jsonArray?.firstOrNull()
            ?.jsonObject?.get("message")?.jsonObject
            ?: error("Backend AI response did not contain a provider message")
        val text = message["content"]?.toString()?.trim('"')?.ifBlank { null }
        val calls = message["tool_calls"]?.jsonArray.orEmpty().map { toolCall ->
            val function = toolCall.jsonObject["function"]?.jsonObject
                ?: error("Backend tool call omitted function")
            val arguments = function["arguments"]?.jsonPrimitive?.content
                ?.let { kotlinx.serialization.json.Json.parseToJsonElement(it).jsonObject }
                ?.mapValues { (_, value) -> value.jsonPrimitive.content }
                .orEmpty()
            GeminiFunctionCall(
                name = function["name"]?.jsonPrimitive?.content
                    ?: error("Backend tool call omitted name"),
                arguments = arguments
            )
        }
        return GeminiResponse(text, calls, buildJsonObject {
            put("role", "model")
            put("parts", kotlinx.serialization.json.JsonArray(emptyList()))
        })
    }
}
