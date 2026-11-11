package com.aurviz.personalagent.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pending_actions",
    indices = [
        Index(value = ["status"]),
        Index(value = ["expiresAt"])
    ]
)
data class PendingActionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val toolName: String,
    val arguments: String,
    val status: String,
    val expiresAt: Long
)
