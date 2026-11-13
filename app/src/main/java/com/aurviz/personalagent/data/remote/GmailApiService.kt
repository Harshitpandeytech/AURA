package com.aurviz.personalagent.data.remote

import android.net.Uri
import com.aurviz.personalagent.security.SecureStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.Base64

data class GmailMessageSummary(val id: String, val threadId: String, val subject: String, val snippet: String)
data class GmailDraft(val to: String, val subject: String, val body: String)

class GmailApiService(
    private val client: OkHttpClient,
    private val secureStorage: SecureStorage
) {
    fun isConnected(): Boolean = secureStorage.getString(ACCESS_TOKEN).isNullOrBlank().not()

    suspend fun importantMessages(maxResults: Int = 10): List<GmailMessageSummary> =
        withContext(Dispatchers.IO) {
            val root = request(
                Request.Builder()
                    .url("https://gmail.googleapis.com/gmail/v1/users/me/messages?labelIds=IMPORTANT&maxResults=$maxResults")
                    .get()
            )
            root["messages"]?.jsonArray.orEmpty().mapNotNull { item ->
                val obj = item.jsonObject
                val id = obj["id"]?.jsonPrimitive?.content ?: return@mapNotNull null
                val message = request(
                    Request.Builder()
                        .url("https://gmail.googleapis.com/gmail/v1/users/me/messages/$id?format=metadata&metadataHeaders=Subject")
                        .get()
                )
                val headers = message["payload"]?.jsonObject?.get("headers")?.jsonArray.orEmpty()
                GmailMessageSummary(
                    id = id,
                    threadId = obj["threadId"]?.jsonPrimitive?.content.orEmpty(),
                    subject = headers.firstOrNull {
                        it.jsonObject["name"]?.jsonPrimitive?.content.equals("Subject", true)
                    }?.jsonObject?.get("value")?.jsonPrimitive?.content.orEmpty(),
                    snippet = message["snippet"]?.jsonPrimitive?.content.orEmpty()
                )
            }
        }

    suspend fun readMessage(id: String): String = withContext(Dispatchers.IO) {
        request(
            Request.Builder()
                .url("https://gmail.googleapis.com/gmail/v1/users/me/messages/$id?format=full")
                .get()
        )["snippet"]?.jsonPrimitive?.content ?: "Email has no readable text."
    }

    suspend fun send(draft: GmailDraft) = withContext(Dispatchers.IO) {
        val raw = "To: ${draft.to}\r\nSubject: ${draft.subject}\r\nContent-Type: text/plain; charset=utf-8\r\n\r\n${draft.body}"
        val encoded = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(raw.toByteArray(Charsets.UTF_8))
        request(
            Request.Builder()
                .url("https://gmail.googleapis.com/gmail/v1/users/me/messages/send")
                .post(
                    buildJsonObject { put("raw", encoded) }
                        .toString().toRequestBody("application/json".toMediaType())
                )
        )
    }

    private fun request(builder: Request.Builder): JsonObject {
        val token = secureStorage.getString(ACCESS_TOKEN)
            ?: error("Gmail is not connected")
        client.newCall(
            builder.addHeader("Authorization", "Bearer " + token).build()
        ).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                if (response.code == 403 && body.contains("SERVICE_DISABLED")) {
                    error(
                        "Gmail API is disabled for this Google Cloud project. " +
                            "Enable Gmail API in Google Cloud Console, then reconnect Gmail."
                    )
                }
                if (response.code == 401) {
                    error("Gmail authorization expired. Disconnect and reconnect Gmail.")
                }
                error("Gmail request failed (${response.code}): $body")
            }
            return if (body.isBlank()) buildJsonObject { } else Json.parseToJsonElement(body).jsonObject
        }
    }

    companion object {
        const val ACCESS_TOKEN = "gmail_access_token"
        const val REFRESH_TOKEN = "gmail_refresh_token"

        fun authorizationUri(
            clientId: String,
            redirectUri: String,
            state: String,
            codeChallenge: String? = null
        ): Uri {
            val builder = Uri.parse("https://accounts.google.com/o/oauth2/v2/auth").buildUpon()
                .appendQueryParameter("client_id", clientId)
                .appendQueryParameter("redirect_uri", redirectUri)
                .appendQueryParameter("response_type", "code")
                .appendQueryParameter("access_type", "offline")
                .appendQueryParameter("prompt", "consent")
                .appendQueryParameter("scope", "https://www.googleapis.com/auth/gmail.modify")
                .appendQueryParameter("state", state)
            codeChallenge?.let {
                builder.appendQueryParameter("code_challenge", it)
                    .appendQueryParameter("code_challenge_method", "S256")
            }
            return builder.build()
        }
    }
}
