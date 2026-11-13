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
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

private fun gmailClientRedirectUri(clientId: String): String =
    if (clientId.endsWith(".apps.googleusercontent.com")) {
        "com.googleusercontent.apps.${clientId.substringBefore(".apps.googleusercontent.com")}:/oauth2redirect"
    } else {
        "com.aurviz.personalagent:/oauth2redirect"
    }

class GmailOAuthManager(
    private val client: OkHttpClient,
    private val secureStorage: SecureStorage,
    private val clientId: String,
    private val redirectUri: String = gmailClientRedirectUri(clientId)
) {
    fun authorizationUri(state: String): Uri {
        require(clientId.isNotBlank()) {
            "Gmail OAuth client ID is not configured. Add GMAIL_CLIENT_ID to local.properties."
        }
        val verifier = ByteArray(32).also { SecureRandom().nextBytes(it) }
            .let { Base64.encodeToString(it, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING) }
        val challenge = Base64.encodeToString(
            MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray()),
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
        secureStorage.putString(OAUTH_STATE, state)
        secureStorage.putString(CODE_VERIFIER, verifier)
        return GmailApiService.authorizationUri(clientId, redirectUri, state, challenge)
    }

    suspend fun exchangeCode(code: String, state: String?): Unit = withContext(Dispatchers.IO) {
        require(clientId.isNotBlank()) { "Gmail OAuth client ID is not configured" }
        require(state == secureStorage.getString(OAUTH_STATE)) {
            "Gmail OAuth state validation failed. Start the connection again."
        }
        val request = Request.Builder()
            .url("https://oauth2.googleapis.com/token")
            .post(
                FormBody.Builder()
                    .add("code", code)
                    .add("client_id", clientId)
                    .add("redirect_uri", redirectUri)
                    .add("grant_type", "authorization_code")
                    .add("code_verifier", secureStorage.getString(CODE_VERIFIER).orEmpty())
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
            secureStorage.remove(OAUTH_STATE)
            secureStorage.remove(CODE_VERIFIER)
        }

        secureStorage.remove(OAUTH_STATE)
        secureStorage.remove(CODE_VERIFIER)
    }

    companion object {
        private const val OAUTH_STATE = "gmail_oauth_state"
        private const val CODE_VERIFIER = "gmail_code_verifier"

    }
}
