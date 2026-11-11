package com.aurviz.personalagent.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "execution_logs",
    indices = [
        Index(value = ["taskId"]),
        Index(value = ["createdAt"]),
        Index(value = ["status"])
    ]
)
data class ExecutionLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val taskId: String,
    val command: String,
    val status: String,
    val result: String,
    val durationMs: Long,
    val createdAt: Long
)
