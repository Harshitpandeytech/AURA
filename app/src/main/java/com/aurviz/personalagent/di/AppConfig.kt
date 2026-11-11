package com.aurviz.personalagent.di

class AppConfig(
    val apiKey: String,
    val gmailClientId: String = ""
) {
    fun isGeminiKeyConfigured(): Boolean = apiKey.isNotBlank()
}
