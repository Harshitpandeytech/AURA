package com.aurviz.personalagent.di

class AppConfig(
    val apiKey: String,
    val gmailClientId: String = "",
    val backendBaseUrl: String = "",
    val geminiModel: String = "gemini-3.8-flash",
    val groqApiKey: String = "",
    val groqModel: String = "llama-3.3-70b-versatile"
) {
    fun isGeminiKeyConfigured(): Boolean = apiKey.isNotBlank()
    fun isGroqKeyConfigured(): Boolean = groqApiKey.isNotBlank()
    fun isBackendConfigured(): Boolean = backendBaseUrl.isNotBlank()
}
