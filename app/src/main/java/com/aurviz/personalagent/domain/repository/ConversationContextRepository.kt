package com.aurviz.personalagent.domain.repository

interface ConversationContextRepository {
    suspend fun get(): String?
    suspend fun save(content: String)
    suspend fun clear()
}
