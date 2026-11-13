package com.aurviz.personalagent.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversation_context")
data class ConversationContextEntity(
    @PrimaryKey val id: Int = 1,
    val content: String,
    val updatedAt: Long
)
