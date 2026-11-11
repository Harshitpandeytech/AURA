package com.aurviz.personalagent.ai

import com.aurviz.personalagent.di.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

data class GeminiFunctionCall(val name: String, val arguments: Map<String, String>)
data class GeminiResponse(val text: String?, val functionCalls: List<GeminiFunctionCall>, val modelContent: JsonObject)

class GeminiAgent(
    private val client: OkHttpClient,
    private val config: AppConfig,
    private val model: String = "gemini-2.0-flash"
) {
    constructor() : this(OkHttpClient(), AppConfig(""))

    val isEnabled: Boolean get() = config.isGeminiKeyConfigured()

    suspend fun complete(prompt: String): String {
        val response = generate(
            listOf(buildJsonObject {
                put("role", "user")
                put("parts", JsonArray(listOf(JsonObject(mapOf("text" to JsonPrimitive(prompt)))))
                )
            }),
            JsonArray(emptyList())
        )
        return response.text ?: error("Gemini returned an empty response")
    }

    suspend fun generate(contents: List<JsonObject>, tools: JsonArray): GeminiResponse = withContext(Dispatchers.IO) {
        check(isEnabled) { "AI integration is not enabled" }
        val payload = buildJsonObject {
            put("contents", JsonArray(contents))
            put("tools", tools)
        }
        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent")
            .header("x-goog-api-key", config.apiKey)
            .post(Json.encodeToString(JsonObject.serializer(), payload).toRequestBody("application/json".toMediaType()))
            .build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                error("Gemini request failed (${response.code}): ${parseError(body)}")
            }
            parseResponse(Json.parseToJsonElement(body).jsonObject)
        }
    }

    private fun parseResponse(root: JsonObject): GeminiResponse {
        val content = root["candidates"]?.jsonArray?.firstOrNull()
            ?.jsonObject?.get("content")?.jsonObject
            ?: error("Gemini response did not contain a candidate")
        val parts = content["parts"]?.jsonArray.orEmpty()
        val text = parts.mapNotNull { it.jsonObject["text"]?.jsonPrimitive?.content }.joinToString("\n").ifBlank { null }
        val calls = parts.mapNotNull { part ->
            part.jsonObject["functionCall"]?.jsonObject?.let { call ->
                GeminiFunctionCall(
                    name = call["name"]?.jsonPrimitive?.content ?: error("Function call has no name"),
                    arguments = call["args"]?.jsonObject.orEmpty().mapValues { (_, value) ->
                        value.jsonPrimitive.content
                    }
                )
            }
        }
        return GeminiResponse(text, calls, content)
    }

    private fun parseError(body: String): String =
        runCatching { Json.parseToJsonElement(body).jsonObject["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content }
            .getOrNull() ?: body.take(300)
}
