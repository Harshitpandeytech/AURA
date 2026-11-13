package com.aurviz.personalagent.ai

import com.aurviz.personalagent.di.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class GroqAgent(
    private val client: OkHttpClient,
    private val config: AppConfig
) {
    val isEnabled: Boolean get() = config.isGroqKeyConfigured()

    suspend fun generate(contents: List<JsonObject>, geminiTools: JsonArray): GeminiResponse =
        withContext(Dispatchers.IO) {
            check(isEnabled) { "Groq integration is not configured." }
            val payload = buildJsonObject {
                put("model", config.groqModel)
                put("messages", JsonArray(toMessages(contents)))
                if (geminiTools.isNotEmpty()) {
                    put("tools", JsonArray(toTools(geminiTools)))
                    put("tool_choice", "auto")
                }
            }
            val request = Request.Builder()
                .url("https://api.groq.com/openai/v1/chat/completions")
                .header("Authorization", "Bearer ${config.groqApiKey}")
                .post(Json.encodeToString(JsonObject.serializer(), payload)
                    .toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    error("Groq request failed (${response.code}): ${parseError(body)}")
                }
                parseResponse(Json.parseToJsonElement(body).jsonObject)
            }
        }

    private fun toMessages(contents: List<JsonObject>): List<JsonObject> =
        contents.flatMap { content ->
            val role = content["role"]?.jsonPrimitive?.content ?: "user"
            content["parts"]?.jsonArray.orEmpty().mapNotNull { part ->
                val partObject = part.jsonObject
                when {
                    partObject["text"] != null -> buildJsonObject {
                        put("role", if (role == "model") "assistant" else role)
                        put("content", partObject["text"]!!.jsonPrimitive.content)
                    }
                    partObject["functionCall"] != null -> {
                        val call = partObject["functionCall"]!!.jsonObject
                        buildJsonObject {
                            put("role", "assistant")
                            putJsonArray("tool_calls") {
                                add(buildJsonObject {
                                    put("id", call["name"]!!.jsonPrimitive.content)
                                    put("type", "function")
                                    putJsonObject("function") {
                                        put("name", call["name"]!!.jsonPrimitive.content)
                                        put("arguments", call["args"]?.toString() ?: "{}")
                                    }
                                })
                            }
                        }
                    }
                    partObject["functionResponse"] != null -> {
                        val function = partObject["functionResponse"]!!.jsonObject
                        buildJsonObject {
                            put("role", "tool")
                            put("tool_call_id", function["name"]!!.jsonPrimitive.content)
                            put(
                                "content",
                                function["response"]?.jsonObject
                                    ?.get("output")?.jsonPrimitive?.content
                                    ?: function["response"]?.jsonObject
                                        ?.get("result")?.jsonPrimitive?.content
                                        .orEmpty()
                            )
                        }
                    }
                    else -> null
                }
            }
        }

    private fun toTools(geminiTools: JsonArray): List<JsonObject> =
        geminiTools.flatMap { declaration ->
            declaration.jsonObject["functionDeclarations"]?.jsonArray.orEmpty().map { function ->
                buildJsonObject {
                    put("type", "function")
                    put("function", function)
                }
            }
        }

    private fun parseResponse(root: JsonObject): GeminiResponse {
        val message = root["choices"]?.jsonArray?.firstOrNull()
            ?.jsonObject?.get("message")?.jsonObject
            ?: error("Groq response did not contain a message.")
        val text = message["content"]?.jsonPrimitive?.content
        val calls = message["tool_calls"]?.jsonArray.orEmpty().map { toolCall ->
            val function = toolCall.jsonObject["function"]!!.jsonObject
            val arguments = Json.parseToJsonElement(
                function["arguments"]?.jsonPrimitive?.content ?: "{}"
            ).jsonObject.mapValues { (_, value) -> value.jsonPrimitive.content }
            GeminiFunctionCall(function["name"]!!.jsonPrimitive.content, arguments)
        }
        val modelContent = buildJsonObject {
            put("role", "model")
            putJsonArray("parts") {
                text?.let { add(buildJsonObject { put("text", it) }) }
                calls.forEach { call ->
                    add(buildJsonObject {
                        putJsonObject("functionCall") {
                            put("name", call.name)
                            put("args", JsonObject(call.arguments.mapValues { Json.parseToJsonElement("\"${it.value.replace("\"", "\\\"")}\"") }))
                        }
                    })
                }
            }
        }
        return GeminiResponse(text, calls, modelContent)
    }

    private fun parseError(body: String): String =
        runCatching { Json.parseToJsonElement(body).jsonObject["error"]?.jsonPrimitive?.content }
            .getOrNull() ?: body.take(300)
}
