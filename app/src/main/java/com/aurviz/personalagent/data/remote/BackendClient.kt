package com.aurviz.personalagent.data.remote

import com.aurviz.personalagent.security.SecureStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class BackendClient(
    private val client: OkHttpClient,
    baseUrl: String,
    private val secureStorage: SecureStorage
) {
    private val baseUrl = baseUrl.trimEnd('/')

    val isConfigured: Boolean
        get() = baseUrl.isNotBlank()

    val isAuthenticated: Boolean
        get() = secureStorage.getString(ACCESS_TOKEN).isNullOrBlank().not()

    suspend fun login(email: String, password: String): String =
        authenticate("/v1/auth/login", email, password)

    suspend fun register(email: String, password: String): String =
        authenticate("/v1/auth/register", email, password)

    suspend fun generate(contents: List<JsonObject>, tools: JsonArray): JsonObject =
        request(
            path = "/v1/ai/generate",
            method = "POST",
            body = buildJsonObject {
                put("contents", JsonArray(contents))
                put("tools", tools)
            }
        )

    fun clearSession() {
        secureStorage.remove(ACCESS_TOKEN)
    }

    private suspend fun authenticate(path: String, email: String, password: String): String =
        request(
            path = path,
            method = "POST",
            body = buildJsonObject {
                put("email", email.trim())
                put("password", password)
            }
        ).jsonObject["accessToken"]?.jsonPrimitive?.content
            ?.also { secureStorage.putString(ACCESS_TOKEN, it) }
            ?: error("Backend authentication response omitted accessToken")

    private suspend fun request(path: String, method: String, body: JsonObject): JsonObject =
        withContext(Dispatchers.IO) {
            require(isConfigured) { "Production backend URL is not configured." }
            val requestBody = Json.encodeToString(JsonObject.serializer(), body)
                .toRequestBody("application/json".toMediaType())
            val requestBuilder = Request.Builder()
                .url("$baseUrl$path")
                .header("Accept", "application/json")
                .method(method, requestBody)
            secureStorage.getString(ACCESS_TOKEN)?.let {
                requestBuilder.header("Authorization", "Bearer $it")
            }
            client.newCall(requestBuilder.build()).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                val json = runCatching { Json.parseToJsonElement(responseBody).jsonObject }.getOrNull()
                if (!response.isSuccessful) {
                    error(json?.get("error")?.jsonPrimitive?.content ?: "Backend request failed (${response.code})")
                }
                json ?: error("Backend returned an invalid response.")
            }
        }

    private companion object {
        const val ACCESS_TOKEN = "backend_access_token"
    }
}
