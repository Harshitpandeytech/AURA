package com.aurviz.personalagent.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "scheduled_tasks",
    indices = [
        Index(value = ["enabled"]),
        Index(value = ["nextRunAt"])
    ]
)
data class ScheduledTaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val instruction: String,
    val scheduleExpression: String,
    val toolConfig: String,
    val enabled: Boolean,
    val nextRunAt: Long,
    val createdAt: Long
)
