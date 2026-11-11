package com.aurviz.personalagent.di

class AppConfig(
    val apiKey: String,
    val gmailClientId: String = "",
    val geminiModel: String = "gemini-3.8-flash"
) {
    fun isGeminiKeyConfigured(): Boolean = apiKey.isNotBlank()
}
