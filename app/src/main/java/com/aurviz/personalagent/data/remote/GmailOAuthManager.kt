package com.aurviz.personalagent.data.remote

import android.net.Uri
import com.aurviz.personalagent.security.SecureStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request

class GmailOAuthManager(
    private val client: OkHttpClient,
    private val secureStorage: SecureStorage,
    private val clientId: String,
    private val redirectUri: String = "com.aurviz.personalagent:/oauth2redirect"
) {
    fun authorizationUri(state: String): Uri =
        GmailApiService.authorizationUri(clientId, redirectUri, state)

    suspend fun exchangeCode(code: String): Unit = withContext(Dispatchers.IO) {
        require(clientId.isNotBlank()) { "Gmail OAuth client ID is not configured" }
        val request = Request.Builder()
            .url("https://oauth2.googleapis.com/token")
            .post(
                FormBody.Builder()
                    .add("code", code)
                    .add("client_id", clientId)
                    .add("redirect_uri", redirectUri)
                    .add("grant_type", "authorization_code")
                    .build()
            )
            .build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) error("Gmail OAuth failed (${response.code}): $body")
            val json = Json.parseToJsonElement(body).jsonObject
            secureStorage.putString(
                GmailApiService.ACCESS_TOKEN,
                json["access_token"]?.jsonPrimitive?.content ?: error("OAuth response omitted access token")
            )
            json["refresh_token"]?.jsonPrimitive?.content?.let {
                secureStorage.putString(GmailApiService.REFRESH_TOKEN, it)
            }
        }
    }
}
