package com.aurviz.personalagent.data.repository

import com.aurviz.personalagent.data.local.dao.ConversationContextDao
import com.aurviz.personalagent.data.local.entity.ConversationContextEntity
import com.aurviz.personalagent.domain.repository.ConversationContextRepository

class ConversationContextRepositoryImpl(
    private val dao: ConversationContextDao
) : ConversationContextRepository {
    override suspend fun get(): String? = dao.get()?.content

    override suspend fun save(content: String) {
        dao.save(
            ConversationContextEntity(
                content = content,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun clear() {
        dao.clear()
    }
}
