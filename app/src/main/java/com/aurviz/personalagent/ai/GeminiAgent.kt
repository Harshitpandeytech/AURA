package com.aurviz.personalagent.ai

import com.aurviz.personalagent.di.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
    private val model: String = config.geminiModel,
    private val fallback: GroqAgent? = null
) {
    constructor() : this(OkHttpClient(), AppConfig(""))

    val isEnabled: Boolean get() = config.isGeminiKeyConfigured() || fallback?.isEnabled == true

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
        if (!config.isGeminiKeyConfigured()) {
            return@withContext fallback?.generate(contents, tools)
                ?: error("AI integration is not enabled")
        }
        val payload = buildJsonObject {
            put("contents", JsonArray(contents))
            put("tools", tools)
        }
        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent")
            .header("x-goog-api-key", config.apiKey)
            .post(Json.encodeToString(JsonObject.serializer(), payload).toRequestBody("application/json".toMediaType()))
            .build()
        try {
            var lastFailure: String? = null
            repeat(MAX_ATTEMPTS) { attempt ->
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    if (response.isSuccessful) {
                        if (body.isBlank()) error("Gemini returned an empty response.")
                        return@withContext parseResponse(Json.parseToJsonElement(body).jsonObject)
                    }
                    lastFailure = "Gemini request failed (${response.code}): ${parseError(body)}"
                    val retryable = response.code == 429 || response.code == 500 || response.code == 502 || response.code == 503
                    if (!retryable || attempt == MAX_ATTEMPTS - 1) {
                        error(lastFailure!!)
                    }
                }
                delay(RETRY_DELAYS_MS[attempt])
            }
            error(lastFailure ?: "Gemini request failed.")
        } catch (error: Exception) {
            if (fallback?.isEnabled == true && error.message?.contains("Gemini request failed (") == true) {
                fallback.generate(contents, tools)
            } else {
                throw error
            }
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

    private companion object {
        const val MAX_ATTEMPTS = 3
        val RETRY_DELAYS_MS = longArrayOf(800, 1_600, 3_200)
    }
}
